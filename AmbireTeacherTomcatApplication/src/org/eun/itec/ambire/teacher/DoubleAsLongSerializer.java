package org.eun.itec.ambire.teacher;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

public class DoubleAsLongSerializer extends JsonSerializer<Double> {

	@Override
	public void serialize(Double val, JsonGenerator generator, SerializerProvider provider) throws IOException, JsonProcessingException {
		generator.writeNumber(val.longValue());
	}

}
