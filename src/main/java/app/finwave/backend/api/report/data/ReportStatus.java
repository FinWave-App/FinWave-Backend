package app.finwave.backend.api.report.data;

public enum ReportStatus {
    FAILED((short) -1),
    IN_PROGRESS((short) 0),
    AVAILABLE((short) 1);

    final short status;

    ReportStatus(short status) {
        this.status = status;
    }

    public short getShort() {
        return status;
    }
}
