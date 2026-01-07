package services;

import java.io.File;
import java.lang.reflect.Field;

import javax.net.ssl.SSLException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.util.JsonFormat;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class ServiceBase {

	public ManagedChannel channel;
	public String JsonResponse;
	public String Message;
	public int Code = 0;
	public String Success = "OK";

	public ServiceBase(String url) throws SSLException {
		String target = url == null ? "" : url;
		File clientFile = new File(System.getProperty("user.dir") + "/src/main/resources/certs/client-cert.crt");
		File clientPassword = new File(System.getProperty("user.dir") + "/src/main/resources/certs/client-key.key");
		File caFile = new File(System.getProperty("user.dir") + "/src/main/resources/certs/ca-cert.crt");

		SslContext ctx = GrpcSslContexts.forClient().trustManager(caFile).keyManager(clientFile, clientPassword)
				.trustManager(InsecureTrustManagerFactory.INSTANCE).build();

		channel = NettyChannelBuilder.forAddress(target, 443).sslContext(ctx).build();
	}

	public ServiceBase(String message, String url) throws SSLException {
		this(url);
		Message = message;
	}

	public void addError(StatusRuntimeException e) {
		Status status = e.getStatus();
		int code = status.getCode().value();
		String description = status.getDescription();
		String exception = status.asRuntimeException().getMessage();
		JsonResponse = "{}";
		addErrorMessage(description, code, exception);
	}

	public void addErrorMessage(String status, int code, String exception) {
		JsonObject jObt = JsonParser.parseString(JsonResponse).getAsJsonObject();
		jObt.addProperty("Status Message", status);
		jObt.addProperty("Status Code", code);
		jObt.addProperty("Status Exception Message", exception);
		JsonResponse = jObt.toString();

	}

	public void addMessage(String status, int code) {
		JsonObject jObt = JsonParser.parseString(JsonResponse).getAsJsonObject();
		jObt.addProperty("Status Message", status);
		jObt.addProperty("Status Code", code);
		JsonResponse = jObt.toString();
	}

	public void setDefaultValues(JsonFormat.Printer printer) {
		try {
			Field field = JsonFormat.Printer.class.getDeclaredField("alwaysOutputDefaultValueFields");
			field.setAccessible(true);
			field.set(printer, true);
		} catch (Exception e) {
			System.out.println("Error setting with Reflection: " + e.getMessage());
		}
	}

	public void printJsonResult() {
		System.out.println(JsonResponse);
	}

	public void validateJsonField(String message, String fieldName, String expectedDataType) {
		String pattern = "";
		switch (expectedDataType.toLowerCase()) {
		case "string":
			pattern = "\"" + fieldName + "\"\\s*:\\s*\"[^\"]*\"";
			break;
		case "int":
			pattern = "\"" + fieldName + "\"\\s*:\\s*(null|[-+]?\\d+)\\s*[,}]";
			break;
		case "long":
			pattern = "\"" + fieldName + "\"\\s*:\\s*(null|\"[-+]?\\d+\"|[-+]?\\d+)\\s*[,}]";
			break;
		case "float":
			pattern = "\"" + fieldName + "\"\\s*:\\s*(null|[-+]?\\d*\\.\\d+([eE][-+]?\\d+)?)\\s*[,}]";
			break;
		case "double":
			pattern = "\"" + fieldName + "\"\\s*:\\s*(null|[-+]?\\d+(\\.\\d+)?([eE][-+]?\\d+)?)\\s*[,}]";
			break;
		case "boolean":
			pattern = "\"" + fieldName + "\"\\s*:\\s*(null|true|false)\\s*[,}]";
			break;
		case "null":
			pattern = "\"" + fieldName + "\"\\s*:\\s*null\\s*[,}]";
			break;
		case "object":
			pattern = "\"" + fieldName + "\"\\s*:\\s*\\{.*?\\}";
			break;
		case "array":
			pattern = "\"" + fieldName + "\"\\s*:\\s*\\[.*?\\]";
			break;
		case "enum":
			pattern = "\"" + fieldName + "\"\\s*:\\s*(\"[^\"]*\"|[-+]?\\d+)\\s*[,}]";
			break;
		default:
			throw new IllegalArgumentException("Unsupported data type for validation: " + expectedDataType);
		}
		if (!message.matches("(?s).*" + pattern + ".*")) {
			throw new IllegalArgumentException(String.format(
					"Field '%s' is not of expected type '%s' or not a valid JSON.", fieldName, expectedDataType));
		}
	}

}
