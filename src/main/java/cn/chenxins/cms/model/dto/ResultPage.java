package cn.chenxins.cms.model.dto;

import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class ResultPage<T> implements Serializable {

    private static final long serialVersionUID = 8422859937779450650L;

    protected List<T> list;
    protected Integer page;
    protected Integer pageSize;
    protected Integer totalPage;
    protected Integer totalRow;

    public ResultPage() {
    }

    public ResultPage(List<T> list, Integer page, Integer pageSize, Integer totalRow) {
        if (null == list) {
            list = new ArrayList<>();
        }
        this.list = list;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPage = totalRow / pageSize;
        if (totalRow % pageSize != 0) {
            this.totalPage++;
        }
        this.totalRow = totalRow;
    }

    public ResultPage(List<T> list, Pager pager, Integer totalRow) {
        this(list, pager.getPage(), pager.getPageSize(), totalRow);
    }

    public ResultPage(List<T> list, Integer page, Integer pageSize, Long totalRow) {
        this(list, page, pageSize, totalRow.intValue());
    }

    public ResultPage(List<T> list, Pager pager, Long totalRow) {
        this(list, pager.getPage(), pager.getPageSize(), totalRow.intValue());
    }

    /**
     * 取出list中的一段转化为ResultPage
     */
    public static <T> ResultPage<T> subList(List<T> objs, Integer page, Integer size) {
        if (objs == null || objs.size() == 0) {
            return new ResultPage<>(Lists.newArrayList(), page, size, 0);
        }
        if (page <= 1 && size > objs.size()) {
            return new ResultPage<>(objs, page, size, objs.size());
        }
        int from = (page - 1) * size;
        if (from >= objs.size()) {
            return new ResultPage<>(Lists.newArrayList(), page, size, objs.size());
        }
        if (from < 0) {
            from = 0;
        }
        int to = from + size;
        if (to < 0 || to >= objs.size()) {
            to = objs.size();
        }
        return new ResultPage<>(objs.subList(from, to), page, size, objs.size());
    }

    public static <T> ResultPage<T> subList(List<T> objs, Pager pager) {
        return subList(objs, pager.getPage(), pager.getPageSize());
    }

    @SuppressWarnings("unchecked")
    public <R> ResultPage<R> replaceList(List<R> newList) {
        return new ResultPage(newList, page, pageSize, totalRow);
    }

    public <R> ResultPage<R> replaceList(Function<? super T, R> mapper) {
        return replaceList(list.stream().map(mapper).collect(Collectors.toList()));
    }

    public static <R> ResultPage<R> emptyResultPage(Pager pager) {
        return new ResultPage<>(Collections.emptyList(), pager, 0);
    }

    public List<T> getList() {
        return list;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getTotalPage() {
        return totalPage;
    }

    public long getTotalRow() {
        return totalRow.longValue();
    }

    public Integer getTotalRow(Integer i) {
        return totalRow;
    }

    public Pager getPager() {
        return new Pager(page, pageSize);
    }

}
