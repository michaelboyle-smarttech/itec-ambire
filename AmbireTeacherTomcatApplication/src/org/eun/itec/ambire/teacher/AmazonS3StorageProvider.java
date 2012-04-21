/*   Copyright (C) 2012, SMART Technologies.
     All rights reserved.
  
     Redistribution and use in source and binary forms, with or without modification, are permitted
     provided that the following conditions are met:
   
      * Redistributions of source code must retain the above copyright notice, this list of
        conditions and the following disclaimer.
   
      * Redistributions in binary form must reproduce the above copyright notice, this list of
        conditions and the following disclaimer in the documentation and/or other materials
        provided with the distribution.
   
      * Neither the name of SMART Technologies nor the names of its contributors may be used to
         endorse or promote products derived from this software without specific prior written
         permission.
   
     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
     IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
     FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
     CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
     CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
     SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
     THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
     OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
     POSSIBILITY OF SUCH DAMAGE.
   
     Author: Michael Boyle
*/
package org.eun.itec.ambire.teacher;

import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;

import javax.servlet.ServletContext;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.resources.S3ObjectResource;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.StorageClass;

public class AmazonS3StorageProvider implements StorageProvider {

	private String ACCOUNT_ID;
	private String ACCESS_KEY;
	private String SECRET_KEY;
	private String REGION;
	private String BUCKET;
	private String ENDPOINT;
	private AmazonS3Client m_client;
	
	public AmazonS3StorageProvider(ServletContext context) {
		ACCOUNT_ID = Deployment.getParameter(context, "AmazonS3StorageProvider.ACCOUNT_ID", AmazonWebServices.ACCOUNT_ID);
		ACCESS_KEY = Deployment.getParameter(context, "AmazonS3StorageProvider.ACCESS_KEY", AmazonWebServices.ACCESS_KEY);
		SECRET_KEY = Deployment.getParameter(context, "AmazonS3StorageProvider.SECRET_KEY", AmazonWebServices.SECRET_KEY);
		REGION = Deployment.getParameter(context, "AmazonS3StorageProvider.REGION", AmazonWebServices.REGION);
		BUCKET = Deployment.getParameter(context, "AmazonS3StorageProvider.BUCKET", "itecambire");
		ENDPOINT = Deployment.getParameter(context, "AmazonS3StorageProvider.ENDPOINT", "s3-" + AmazonWebServices.ENDPOINT);
		if(ENDPOINT != null && ENDPOINT.length() == 0) {
			ENDPOINT = null;
		}
		AmazonS3Client s3 = open();
		try {
			if(!s3.doesBucketExist(BUCKET)) {
				s3.createBucket(BUCKET, Region.valueOf(REGION));
				s3.setBucketPolicy(BUCKET, new Policy().withStatements(
					new Statement(Effect.Allow)
						.withPrincipals(Principal.AllUsers)
						.withActions(S3Actions.GetObject)
						.withResources(new S3ObjectResource(BUCKET, "*")),
					new Statement(Effect.Allow)
				        .withPrincipals(new Principal(ACCOUNT_ID))
				        .withActions(S3Actions.PutObject)
				        .withResources(new S3ObjectResource(BUCKET, "*"))).toJson());
			}
			m_client = s3;
		} catch(Exception e) {
			e.printStackTrace();
			close(s3);
		}
	}

	private AmazonS3Client open() {
		try {
			AmazonS3Client s3 = new AmazonS3Client(new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY));
			try {
				if(ENDPOINT != null) {
					s3.setEndpoint(ENDPOINT);
				}
				return s3;
			} catch(Exception e) {
				close(s3);
			}
		} catch(Exception e) {}
		return null;
	}
	
	private void close(AmazonS3Client s3) {
		if(s3 != null) {
			try {
				s3.shutdown();
			} catch(Exception e) {}
		}
	}
	
	@Override
	public StoredFileInfo storeFile(InputStream content, long contentLength, String contentType, String suggestedFileName, String baseUrl) {
		if(m_client == null) {
			return null;
		}
		double uploadId = AmazonWebServices.identity();
		String key = String.format("_%d%s", (long)uploadId, Deployment.extensionForMimeType(contentType));
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType(contentType);
		metadata.setContentLength(contentLength);
		Date expiration = new Date(System.currentTimeMillis() + (long)Deployment.MAX_UPLOAD_AGE_MILLIS);
		metadata.setExpirationTime(expiration);
		try {
			PutObjectRequest request = new PutObjectRequest(BUCKET, key, content, metadata);
			request.setStorageClass(StorageClass.ReducedRedundancy);
			m_client.putObject(request);
			StoredFileInfo stored = new StoredFileInfo();
			stored.href = m_client.generatePresignedUrl(BUCKET, key, expiration).toString();
			stored.token = key;
			return stored;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void deleteFiles(Iterable<String> tokens, boolean force) {
		if(force) {
			try {
				LinkedList<DeleteObjectsRequest.KeyVersion> keys = new LinkedList<DeleteObjectsRequest.KeyVersion>();
				for(String token : tokens) {
					keys.add(new DeleteObjectsRequest.KeyVersion(token));
				}
				m_client.deleteObjects(new DeleteObjectsRequest(BUCKET)
					.withKeys(keys)
				);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void close() {
		close(m_client);
		m_client = null;
	}

}
