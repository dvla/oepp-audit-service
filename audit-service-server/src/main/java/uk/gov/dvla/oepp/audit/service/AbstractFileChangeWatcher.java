package uk.gov.dvla.oepp.audit.service;

import java.io.File;
import java.util.TimerTask;

public abstract class AbstractFileChangeWatcher extends TimerTask {

    private final File file;
    private long lastKnownFileModificationTime;

    public AbstractFileChangeWatcher(File file) {
        this.file = file;
        this.lastKnownFileModificationTime = file.lastModified();
    }

    @Override
    public void run() {
        long fileModificationTime = file.lastModified();

        if(this.lastKnownFileModificationTime != fileModificationTime) {
            this.lastKnownFileModificationTime = fileModificationTime;
            onChange(file);
        }
    }

    protected abstract void onChange(File file);
}
