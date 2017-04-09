/*import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;*/
import java.util.Date;
import java.util.logging.*;
import java.io.*;
import java.text.SimpleDateFormat;

class LogFormatter extends Formatter {
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

public class MyLogger {
	static private FileHandler logTxt;
	static private LogFormatter formatterText;
	private static Logger logger;

	static public void setup() throws IOException {

		logger = Logger.getLogger(MyLogger.class.getName());
		logger.setUseParentHandlers(false);

		// suppress the logging output to the console
		// Logger rootLogger = Logger.getGlobal();
		Handler[] handlers = logger.getHandlers();
		if (handlers != null && handlers.length > 0 && handlers[0] instanceof ConsoleHandler) {
			logger.removeHandler(handlers[0]);
		}

		logger.setLevel(Level.INFO);
		String logDirName = "peer_" + CommonPeerConfig.retrieveCommonConfig().get("peerId");
        File directory = new File(logDirName);
        if(!directory.isDirectory())
            directory.mkdir();
        String logFileName =  directory.getPath() + File.separator + "log_peer_"
                + CommonPeerConfig.retrieveCommonConfig().get("peerId") + ".log";
        logTxt = new FileHandler(logFileName);
		formatterText = new LogFormatter();
		logTxt.setFormatter(formatterText);
		logger.addHandler(logTxt);
	}

	public static Logger getMyLogger() {
		return logger;
	}
}

