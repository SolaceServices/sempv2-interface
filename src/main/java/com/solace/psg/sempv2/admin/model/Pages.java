package com.solace.psg.sempv2.admin.model;


import com.google.gson.annotations.SerializedName;

@javax.annotation.Resource
public class Pages {

    @SerializedName("next-page")
    private Object nextPage;
    @SerializedName("total-pages")
    private Long totalPages;

    public Object getNextPage() {
        return nextPage;
    }

    public void setNextPage(Object nextPage) {
        this.nextPage = nextPage;
    }

    public Long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Long totalPages) {
        this.totalPages = totalPages;
    }

}
