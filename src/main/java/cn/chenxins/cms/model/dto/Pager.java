package cn.chenxins.cms.model.dto;


/**
 * User: Michael Chen Email: yidongnan@gmail.com Date: 2014/3/26 Time: 15:59
 */
public class Pager {
    private int page = 1;
    protected int pageSize = 10;

    public Pager() {
    }

    public Pager(int page, int pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }

    public int getPage() {
        return page > 0 ? page : 1;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize > 0 ? pageSize : 10;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getOffset() {
        return page > 0 ? (page - 1) * pageSize : 0;
    }
}
