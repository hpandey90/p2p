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
		StringBuffer strBuffer = new StringBuffer(1000);
		SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
		Date resultdate = new Date(rc.getMillis());
		strBuffer.append(date_format.format(resultdate));
		strBuffer.append(" ");
		strBuffer.append(rc.getMessage());
		strBuffer.append("\n");
		return strBuffer.toString();
	}
}

public class MyLogger {
	static private FileHandler logFileHandler;
	static private LogFormatter logTextHandler;
	private static Logger logger;

	static public void setup() throws IOException {

		logger = Logger.getLogger(MyLogger.class.getName());
		logger.setUseParentHandlers(false);
		Handler[] handlers = logger.getHandlers();
		if (handlers.length > 0 && handlers != null && handlers[0] instanceof ConsoleHandler) {
			logger.removeHandler(handlers[0]);
		}

		logger.setLevel(Level.INFO);
		String logDirName = "peer_" + CommonPeerConfig.retrieveCommonConfig().get("peerId");
        File directory = new File(logDirName);
        if(!directory.isDirectory()){
            directory.mkdir();
        }
        logTextHandler = new LogFormatter();
        String logFileName =  directory.getPath() + File.separator + "log_peer_"+ CommonPeerConfig.retrieveCommonConfig().get("peerId") + ".log";
        logFileHandler = new FileHandler(logFileName);
		logFileHandler.setFormatter(logTextHandler);
		logger.addHandler(logFileHandler);
	}

	public static Logger getLogger() {
		return logger;
	}
}

