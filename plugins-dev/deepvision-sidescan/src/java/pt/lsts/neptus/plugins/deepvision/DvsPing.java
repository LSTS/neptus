package pt.lsts.neptus.plugins.deepvision;

public class DvsPing {
    private DvsPos dvsPos;
    private DvsReturn dvsReturn;

    public DvsPing(DvsPos dvsPos, DvsReturn dvsReturn) {
        this.dvsPos = dvsPos;
        this.dvsReturn = dvsReturn;
    }

    public DvsPos getDvsPos() {
        return dvsPos;
    }

    public DvsReturn getDvsReturn() {
        return dvsReturn;
    }
}
