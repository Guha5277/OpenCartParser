package com.guhar4k.parser;

public interface IGrabber extends IParser{
    void onGrabberReady();
    void onGrabError();
    void onGrabberException(String message);
    void onGrabberSuccessfulEnd(int count);

    void insertCategory(String name);
    void insertStore(int region, String city, String address, String phone);
}
