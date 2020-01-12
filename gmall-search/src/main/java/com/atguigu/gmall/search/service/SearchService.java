package com.atguigu.gmall.search.service;

import com.atguigu.gmall.search.pojo.SearchParam;
import com.atguigu.gmall.search.pojo.SearchResponseVO;

import java.io.IOException;

public interface SearchService {

    SearchResponseVO search(SearchParam searchParam) throws IOException;
}
