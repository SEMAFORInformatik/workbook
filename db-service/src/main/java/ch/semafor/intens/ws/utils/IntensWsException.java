package ch.semafor.intens.ws.utils;

import org.springframework.http.HttpStatus;
import java.util.Map;

public class IntensWsException extends RuntimeException {

	/**
   * 
   */
  private static final long serialVersionUID = 1L;
  private HttpStatus status;
	private String message;
	private Map<String, Object> data;

	public IntensWsException( String msg, HttpStatus status) {
		this.setMessage(msg);
		this.setStatus(status);
	}

	public IntensWsException( Throwable e, HttpStatus status) {
		this.setMessage(e.getMessage());
		this.setStatus(status);
	}

	public IntensWsException(Map<String, Object> map, HttpStatus status) {
		this.setData(map);
		this.setStatus(status);
	}

	public HttpStatus getStatus() {
		return status;
	}

	public int getStatusCode() {
		return status.value();
	}

	public void setStatus(HttpStatus status) {
		this.status = status;
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}
}
