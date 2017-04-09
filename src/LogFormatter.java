import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
	public String format(LogRecord rc) {
		StringBuffer buf = new StringBuffer(1000);
		SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
		Date resultdate = new Date(rc.getMillis());
		buf.append(date_format.format(resultdate));
		buf.append(" ");
		buf.append(rc.getMessage());
		buf.append("\n");
		return buf.toString();
	}


}
