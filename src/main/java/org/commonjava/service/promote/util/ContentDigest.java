package org.commonjava.service.promote.util;

public enum ContentDigest {
    MD5,
    SHA_512( "SHA-512" ),
    SHA_384( "SHA-384" ),
    SHA_256( "SHA-256" ),
    SHA_1 ( "SHA-1" );

    private String digestName;

    ContentDigest()
    {
        this.digestName = name();
    }

    ContentDigest( final String digestName )
    {
        this.digestName = digestName;
    }

    public String digestName()
    {
        return digestName;
    }
}
