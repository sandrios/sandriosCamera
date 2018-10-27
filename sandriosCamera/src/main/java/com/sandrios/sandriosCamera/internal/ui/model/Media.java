package com.sandrios.sandriosCamera.internal.ui.model;

import android.net.Uri;

import java.io.Serializable;

/**
 * Created by Arpit Gandhi
 */

public class Media implements Serializable {
    private int type;
    private Uri uri;

    public Media() {
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
