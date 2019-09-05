package com.redhat.rhjmc.prometheus_jfr_exporter;

import org.openjdk.jmc.ui.common.security.ActionNotGrantedException;
import org.openjdk.jmc.ui.common.security.FailedToSaveException;
import org.openjdk.jmc.ui.common.security.ISecurityManager;
import org.openjdk.jmc.ui.common.security.SecurityException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SecurityManager implements ISecurityManager {

    static class NotImplementedException extends UnsupportedOperationException {
        NotImplementedException(String msg) {
            super(msg);
        }
    }

    public class Store {

        private final Map<String, Object> store = new HashMap<String, Object>();

        private static final String SEP = "_";

        public String insert(String key, boolean keyFamily, String value) {
            return insertInternal(key, keyFamily, value);
        }

        public String insert(String key, boolean keyFamily, String[] value) {
            return insertInternal(key, keyFamily, value);
        }

        public String insert(String key, boolean keyFamily, byte[] value) {
            return insertInternal(key, keyFamily, value);
        }

        private String generateKey(String family) {
            return (this.store.size() + 1) + SEP + "store" + (family == null ? "" : SEP + family);
        }

        private synchronized String insertInternal(String key, boolean keyFamily, Object value) {
            key = keyFamily || key == null ? generateKey(key) : key;
            this.store.put(key, value);
            return key;
        }

        public synchronized Object get(String key) {
            return this.store.get(key);
        }

        public synchronized void clearFamily(String family, Set<String> keepKeys) {
            Iterator<String> it = this.store.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                String[] keyParts = key.split(SEP);
                if (keyParts.length == 3 && keyParts[2].equals(family) && !keepKeys.contains(key)) {
                    it.remove();
                }
            }
        }

        public synchronized boolean hasKey(String key) {
            return this.store.containsKey(key);
        }

        public synchronized Object remove(String key) {
            return this.store.remove(key);
        }

    }


    private final Store store;

    public SecurityManager() {
        this.store = new Store();
    }

    @Override
    public boolean hasKey(String key) {
        return this.store.hasKey(key);
    }

    @Override
    public Object withdraw(String key) throws SecurityException {
        return this.store.remove(key);
    }

    @Override
    public void clearFamily(String family, Set<String> keys) throws FailedToSaveException {
        this.store.clearFamily(family, keys);
    }

    @Override
    public Object get(String key) throws SecurityException {
        return hasKey(key) ? this.store.get(key) : null;
    }

    @Override
    public String store(byte ... value) throws SecurityException {
        return this.store.insert(null, true, value);
    }

    @Override
    public String store(String ... value) throws SecurityException {
        return this.store.insert(null, true, value);
    }

    @Override
    public String storeInFamily(String family, byte ... value) throws SecurityException {
        return this.store.insert(family, true, value);
    }

    @Override
    public String storeInFamily(String family, String ... value) throws SecurityException {
        return this.store.insert(family, true, value);
    }

    @Override
    public void storeWithKey(String key, byte ... value) throws SecurityException {
        this.store.insert(key, false, value);
    }

    @Override
    public void storeWithKey(String key, String ... value) throws SecurityException {
        this.store.insert(key, false, value);
    }

    @Override
    public Set<String> getEncryptionCiphers() {
        return Collections.emptySet();
    }

    @Override
    public String getEncryptionCipher() {
        return null;
    }

    @Override
    public void setEncryptionCipher(String encryptionCipher) throws SecurityException {
        throw new NotImplementedException("Encryption not supported");
    }

    @Override
    public void changeMasterPassword() throws SecurityException {
        throw new NotImplementedException("Master Password change not implemented");
    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public void unlock() throws ActionNotGrantedException {
        // no-op
    }

}
