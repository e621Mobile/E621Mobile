package info.beastarman.e621.backend.errorReport;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Douglas on 27/03/2016.
 */
public interface ErrorReportManagerInterface {
    void sendReport(ErrorReportReport report);

    void sendPendingReports();

    ArrayList<ErrorReportMessage> getAndUpdateUnreadMessages();

    ArrayList<ErrorReportMessage> getUnreadMessages(boolean update);

    int hasUnreadMessages(String report);

    ArrayList<ErrorReportReport> getReports();

    ErrorReportReport getReport(String reportHash);

    ErrorReportGetMessagesResponse getMessages(String hash) throws IOException;

    void sendMessage(String hash, String text) throws IOException;
}
