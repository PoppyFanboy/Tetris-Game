package poppyfanboy.tetrisgame.states;

public enum Resolution {
    _1024x800, _512x400;

    public int getWidth() {
        switch (this) {
            case _1024x800:
                return 1024;
            case _512x400:
                return 512;
        }
        return -1;
    }

    public int getTileWidth() {
        return getWidth() / getBlockWidth();
    }

    public int getHeight() {
        switch (this) {
            case _1024x800:
                return 800;
            case _512x400:
                return 400;
        }
        return -1;
    }

    public int getTileHeight() {
        return getHeight() / getBlockWidth();
    }

    public int getBlockWidth() {
        switch (this) {
            case _1024x800:
                return 32;
            case _512x400:
                return 16;
        }
        return -1;
    }

    public int getFontSize() {
        switch (this) {
            case _1024x800:
                return 25;
            case _512x400:
                return 12;
        }
        return -1;
    }
}

