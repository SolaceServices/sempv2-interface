package com.solace.psg.sempv2.admin.model;


import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class Meta {

    @Expose
    private Long count;
    @Expose
    private Long currentTime;
    @Expose
    private Long pageNumber;
    @Expose
    private Long pageSize;
    @Expose
    private Pages pages;

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(Long currentTime) {
        this.currentTime = currentTime;
    }

    public Long getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Long pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }

    public Pages getPages() {
        return pages;
    }

    public void setPages(Pages pages) {
        this.pages = pages;
    }

}
