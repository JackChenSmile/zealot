package com.blinkfox.zealot.core.concrete;

import com.blinkfox.zealot.bean.BuildSource;
import com.blinkfox.zealot.bean.SqlInfo;
import com.blinkfox.zealot.consts.ZealotConst;
import com.blinkfox.zealot.core.IConditHandler;
import com.blinkfox.zealot.helpers.ParseHelper;
import com.blinkfox.zealot.helpers.StringHelper;
import com.blinkfox.zealot.helpers.XmlNodeHelper;
import com.blinkfox.zealot.log.Log;

import org.dom4j.Node;

/**
 * choose条件选择标签对应的动态条件选择标签，用来生成SqlInfo片段处理器的实现类.
 * <p>choose标签的主要内容：'[choose when="" then="" when2="" then2="" whenx="" thenx="" else="" /]'</p>
 * @author blinkfox on 2017/8/16.
 */
public class ChooseHandler implements IConditHandler {

    private static final Log log = Log.get(ChooseHandler.class);

    /**
     * 构建`case when`条件选择生成sqlInfo的片段信息.
     * @param source 构建所需的资源对象
     * @return 返回SqlInfo对象
     */
    @Override
    public SqlInfo buildSqlInfo(BuildSource source) {
        Object paramObj = source.getParamObj();
        SqlInfo sqlInfo = source.getSqlInfo();
        Node node = source.getNode();

        // 循环判断所有`when`的值，直到能找到第n个when的时候，如果都为false，则跳出循环走else的逻辑
        // 循环过程中，如果判断到第x个when解析后的值为true时，则解析拼接这第x个then的值到Sql中，并直接返回.
        int i = 0;
        while (true) {
            // 每次循环加1，如果是第一个，则，when/then等添加的后缀为空字符串.
            i++;
            String x = i == 1 ? "" : String.valueOf(i);

            // 获取第i个when属性的文本值，如果其文本内容为空则不再拼接，进入else的分支条件来拼接Sql片段信息.
            String whenText = XmlNodeHelper.getNodeAttrText(node, ZealotConst.ATTR_WHEN + x);
            if (StringHelper.isBlank(whenText)) {
                log.info("case标签中第" + i + "个when属性不存在或者内容为空，进入else的分支条件.");
                break;
            }

            // 如果when属性的解析值为true,则拼接其对应的`then`块的SQL片段.then块的值为解析其字符串模板的值.
            if (ParseHelper.isTrue(whenText, paramObj)) {
                String thenText = XmlNodeHelper.getNodeAttrText(node, ZealotConst.ATTR_THEN + x);
                if (StringHelper.isNotBlank(thenText)) {
                    sqlInfo.getJoin().append(ParseHelper.parseTemplate(thenText, paramObj));
                }
                return sqlInfo;
            }
        }

        // 如果没进入前面任何一个when-then的分支块，则进入else的分支，如果else为空，则直接返回原SqlInfo.
        String elseText = XmlNodeHelper.getNodeAttrText(node, ZealotConst.ATTR_ELSE);
        if (StringHelper.isBlank(elseText)) {
            log.info("case标签中else属性不存在或者内容为空，不拼接任何SQL片段.");
            return sqlInfo;
        }

        sqlInfo.getJoin().append(ParseHelper.parseTemplate(elseText, paramObj));
        return sqlInfo;
    }

}