package info.beastarman.e621.backend.errorReport;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Douglas on 27/03/2016.
 */
public class DummyErrorReportManager implements ErrorReportManagerInterface {
    @Override
    public void sendReport(ErrorReportReport report) {

    }

    @Override
    public void sendPendingReports() {

    }

    @Override
    public ArrayList<ErrorReportMessage> getAndUpdateUnreadMessages() {
        return new ArrayList<ErrorReportMessage>();
    }

    @Override
    public ArrayList<ErrorReportMessage> getUnreadMessages(boolean update) {
        return new ArrayList<ErrorReportMessage>();
    }

    @Override
    public int hasUnreadMessages(String report) {
        return 0;
    }

    @Override
    public ArrayList<ErrorReportReport> getReports() {
        return new ArrayList<ErrorReportReport>();
    }

    @Override
    public ErrorReportReport getReport(String reportHash) {
        return new ErrorReportReport();
    }

    @Override
    public ErrorReportGetMessagesResponse getMessages(String hash) throws IOException {
        return new ErrorReportGetMessagesResponse(new JSONObject());
    }

    @Override
    public void sendMessage(String hash, String text) throws IOException {

    }
}
