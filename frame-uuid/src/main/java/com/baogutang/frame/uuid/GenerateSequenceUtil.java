package com.baogutang.frame.uuid;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GenerateSequenceUtil {

  /** 锁对象，可以为任意对象 */
  private static Object lockObj = "lockerOrder";
  /** 订单号生成计数器 */
  public static long orderNumCount = 0L;
  /** 每毫秒生成订单号数量最大值 */
  public static int maxPerMSECSize = 1000;

  /** 生成非重复订单号，理论上限1毫秒1000个，可扩展 暂时26位数 */
  public static String generateSequenceNo() {
    String finOrderNum = "";
    try {
      // 最终生成的订单号
      synchronized (lockObj) {
        // 取系统当前时间作为订单号变量前半部分，精确到毫秒
        long nowLong = Long.parseLong(new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()));
        // 计数器到最大值归零，可扩展更大，目前1毫秒处理峰值1000个，1秒100万
        if (orderNumCount > maxPerMSECSize) {
          orderNumCount = 0L;
        }
        // 组装订单号
        String countStr = maxPerMSECSize + orderNumCount + "";
        finOrderNum =
            nowLong
                + countStr.substring(1)
                + String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        orderNumCount++;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return finOrderNum;
  }

}
