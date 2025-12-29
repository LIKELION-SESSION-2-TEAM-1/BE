package g3pjt.service.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {
    /** Example: https://<project-ref>.supabase.co */
    private String url;

    /** Service role key (server-side only). */
    private String serviceRoleKey;

    private Storage storage = new Storage();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceRoleKey() {
        return serviceRoleKey;
    }

    public void setServiceRoleKey(String serviceRoleKey) {
        this.serviceRoleKey = serviceRoleKey;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public static class Storage {
        /** Bucket name, e.g. tokplan */
        private String bucket;

        /** If true, return public URL. If false, you must use signed URLs on the client. */
        private boolean publicBucket = true;

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public boolean isPublicBucket() {
            return publicBucket;
        }

        public void setPublicBucket(boolean publicBucket) {
            this.publicBucket = publicBucket;
        }
    }
}
