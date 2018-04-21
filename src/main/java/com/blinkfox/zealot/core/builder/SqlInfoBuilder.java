package com.blinkfox.zealot.core.builder;

import com.blinkfox.zealot.bean.BuildSource;
import com.blinkfox.zealot.bean.SqlInfo;
import com.blinkfox.zealot.consts.ZealotConst;

import java.util.List;

/**
 * 构建拼接sql语句片段和参数的类.
 * Created by blinkfox on 2017-03-31.
 */
public class SqlInfoBuilder {

    /** sqlInfo对象. */
    SqlInfo sqlInfo;

    /** sql拼接器，sqlInfo对象的属性. */
    private StringBuilder join;

    /** 有序的参数集合，sqlInfo对象的属性. */
    private List<Object> params;

    /** 上下文参数. */
    Object context;

    /** 前缀. */
    private String prefix;

    /**
     * 私有构造方法.
     */
    SqlInfoBuilder() {
        super();
    }

    /**
     * 获取JavaSqlInfoBuilder的实例，并初始化属性信息.
     * @param source BuildSource实例
     * @return XmlSqlInfoBuilder实例
     */
    public static SqlInfoBuilder newInstace(BuildSource source) {
        SqlInfoBuilder builder = new SqlInfoBuilder();
        builder.init(source);
        return builder;
    }

    /**
     * 根据构建的资源参数初始化数据.
     * @param source 构建sql的相关参数
     */
    void init(BuildSource source) {
        this.sqlInfo = source.getSqlInfo();
        this.join = sqlInfo.getJoin();
        this.params = sqlInfo.getParams();
        this.context = source.getParamObj();
        this.prefix = source.getPrefix();
    }

    /**
     * 构建普通查询需要的SqlInfo信息.
     * @param fieldText 数据库字段的文本
     * @param value 参数值
     * @param suffix 后缀，如：大于、等于、小于等
     * @return sqlInfo
     */
    public SqlInfo buildNormalSql(String fieldText, Object value, String suffix) {
        join.append(prefix).append(fieldText).append(suffix);
        params.add(value);
        return sqlInfo.setJoin(join).setParams(params);
    }

    /**
     * 根据字段、值和后缀操作符参数来构建like模糊查询需要的SqlInfo信息.
     * @param fieldText 数据库字段的文本
     * @param value 参数值
     * @param suffix 后缀符
     * @return sqlInfo
     */
    private SqlInfo buildLikeSqlBySuffix(String fieldText, Object value, String suffix) {
        join.append(prefix).append(fieldText).append(suffix);
        params.add("%" + value + "%");
        return sqlInfo.setJoin(join).setParams(params);
    }

    /**
     * 构建like模糊查询需要的SqlInfo信息.
     * @param fieldText 数据库字段的文本
     * @param value 参数值
     * @return sqlInfo
     */
    public SqlInfo buildLikeSql(String fieldText, Object value) {
        return this.buildLikeSqlBySuffix(fieldText, value, ZealotConst.LIEK_SUFFIX);
    }

    /**
     * 构建not like模糊查询需要的SqlInfo信息.
     * @param fieldText 数据库字段的文本
     * @param value 参数值
     * @return sqlInfo
     */
    public SqlInfo buildNotLikeSql(String fieldText, Object value) {
        return this.buildLikeSqlBySuffix(fieldText, value, ZealotConst.NOT_LIEK_SUFFIX);
    }

    /**
     * 根据指定的模式`pattern`来构建like模糊查询需要的SqlInfo信息.
     * @param fieldText 数据库字段的文本
     * @param pattern like匹配的模式
     * @return sqlInfo
     */
    public SqlInfo buildLikePatternSql(String fieldText, String pattern) {
        join.append(prefix).append(fieldText).append(ZealotConst.LIEK_KEY)
                .append("'").append(pattern).append("' ");
        return sqlInfo.setJoin(join).setParams(params);
    }

    /**
     * 根据指定的模式`pattern`来构建not like模糊查询需要的SqlInfo信息.
     * @param fieldText 数据库字段的文本
     * @param pattern like匹配的模式
     * @return sqlInfo
     */
    public SqlInfo buildNotLikePatternSql(String fieldText, String pattern) {
        join.append(prefix).append(fieldText).append(ZealotConst.NOT_LIEK_KEY)
                .append("'").append(pattern).append("' ");
        return sqlInfo.setJoin(join).setParams(params);
    }

    /**
     * 构建区间查询的SqlInfo信息.
     * @param fieldText 数据库字段文本
     * @param startValue 参数开始值
     * @param endValue 参数结束值
     * @return 返回SqlInfo信息
     */
    public SqlInfo buildBetweenSql(String fieldText, Object startValue, Object endValue) {
        /* 根据开始文本和结束文本判断执行是大于、小于还是区间的查询sql和参数的生成 */
        if (startValue != null && endValue == null) { // 开始不为空，结束为空的情况
            join.append(prefix).append(fieldText).append(ZealotConst.GTE_SUFFIX);
            params.add(startValue);
        } else if (startValue == null && endValue != null) { // 开始为空，结束不为空的情况
            join.append(prefix).append(fieldText).append(ZealotConst.LTE_SUFFIX);
            params.add(endValue);
        } else { // 开始、结束均不为空的情况
            join.append(prefix).append(fieldText).append(ZealotConst.BT_AND_SUFFIX);
            params.add(startValue);
            params.add(endValue);
        }

        return sqlInfo.setJoin(join).setParams(params);
    }

    /**
     * 构建区间查询的SqlInfo信息.
     * @param fieldText 数据库字段文本
     * @param values 对象数组的值
     * @return 返回SqlInfo信息
     */
    public SqlInfo buildInSql(String fieldText, Object[] values) {
        if (values == null || values.length == 0) {
            return sqlInfo;
        }

        // 遍历数组，并遍历添加in查询的替换符和参数
        join.append(prefix).append(fieldText).append(ZealotConst.IN_SUFFIX).append("(");
        int len = values.length;
        for (int i = 0; i < len; i++) {
            if (i == len - 1) {
                join.append("?) ");
            } else {
                join.append("?, ");
            }
            params.add(values[i]);
        }

        return sqlInfo.setJoin(join).setParams(params);
    }

}