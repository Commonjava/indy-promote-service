package org.commonjava.service.promote.util;

public enum ContentDigest {
    MD5( "MD5", ".md5"),
    SHA_512( "SHA-512", ".sha512"),
    SHA_384( "SHA-384", ".sha384"),
    SHA_256( "SHA-256", ".sha256"),
    SHA_1 ( "SHA-1", ".sha1");

    private String digestName;

    final private String fileExt;

    ContentDigest(final String digestName, String fileExt)
    {
        this.digestName = digestName;
        this.fileExt = fileExt;
    }

    public String digestName()
    {
        return digestName;
    }

    public String getFileExt() {
        return fileExt;
    }
}
