import java.time.LocalDate;

/**
 * [{"allDay":true,"title":"Prenota ora 305","start":"2021-08-04","end":"2021-08-04"},{"allDay":true,"title":"Prenota ora 171","start":"2021-08-03","end":"2021-08-03"}]
 */
public class DayAvailabilities {

    private String allDay;
    private String title;
    private LocalDate start;
    private LocalDate end;

    private String sede;
    private String ulss;

    private boolean notify = true;
    private boolean ignored = false;

    @Override
    public String toString() {
        return " GIORNO= " + start + " SEDE=" + ScanAvailabilities.ulss_e_sedi.get(ulss).get(sede) + "ULSS=" + ScanAvailabilities.ulssLabels.get(ulss) + " follow=" + ScanAvailabilities.ulssLinks.get(ulss);
    }

    public String getAllDay() {
        return allDay;
    }

    public void setAllDay(String allDay) {
        this.allDay = allDay;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getStart() {
        return start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void setEnd(LocalDate end) {
        this.end = end;
    }

    public String getSede() {
        return sede;
    }

    public void setSede(String sede) {
        this.sede = sede;
    }

    public String getUlss() {
        return ulss;
    }

    public void setUlss(String ulss) {
        this.ulss = ulss;
    }

    public boolean isNotify() {
        return notify;
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }
}
