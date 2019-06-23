package com.sandrios.sandriosCamera.internal.ui.model;

import java.io.Serializable;

/**
 * Created by Arpit Gandhi
 */

public class Media implements Serializable {
    private int type;
    private String path;

    public Media(int type, String path) {
        this.type = type;
        this.path = path;
    }

    public Media() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * User media type @link{MediaType}
     *
     * @return
     */
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
