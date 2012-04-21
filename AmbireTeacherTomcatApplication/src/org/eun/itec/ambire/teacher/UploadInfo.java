package org.eun.itec.ambire.teacher;

import org.codehaus.jackson.map.annotate.JsonSerialize;

public class UploadInfo {
	@JsonSerialize(using=DoubleAsLongSerializer.class)
	public double uploadId;
	public String href;
	public String name;
	public String kind;
	@JsonSerialize(using=DoubleAsLongSerializer.class)
	public double timestamp;
	public int width;
	public int height;
}
