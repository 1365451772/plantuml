package com.peng.cmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Author sp
 * @Description
 * @create
 * @Modified By:
 */
public class CmdUtils {

  /**
   * 执行脚本
   * @param shell
   * @return
   * @throws Exception
   */
  public static Integer execute(String shell) throws Exception{
    Runtime runtime = Runtime.getRuntime();
    Process process = runtime.exec(shell);
    ProcessClearStream error=new ProcessClearStream(process.getErrorStream(), "Error");
    ProcessClearStream output= new ProcessClearStream(process.getInputStream(),"OutPut");
    error.start();
    output.start();
    Callable<Integer> call=new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        Integer status=process.waitFor();
        return status;
      }
    };
    ScheduledExecutorService singleThreadScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    Future<Integer> submit = singleThreadScheduledExecutor.submit(call);
    //5分钟脚本未执行完成认为线程阻塞了，强制结束线程
    Integer status = submit.get(5, TimeUnit.MINUTES);
    return status;
  }

  //一般的执行方法，有时执行exe会卡在那    stmt要执行的命令
  public static void executive(String stmt) throws IOException, InterruptedException {
    Runtime runtime = Runtime.getRuntime();  //获取Runtime实例
    //执行命令
    try {
      String[] command = {"cmd", "/c", stmt};
      Process process = runtime.exec(command);
      // 标准输入流（必须写在 waitFor 之前）
      String inStr = consumeInputStream(process.getInputStream());
      // 标准错误流（必须写在 waitFor 之前）
      String errStr = consumeInputStream(process.getErrorStream());
      new ProcessClearStream(process.getInputStream(), "INFO").start();
      new ProcessClearStream(process.getErrorStream(), "ERROR").start();
      int proc = process.waitFor();
      InputStream errorStream = process.getErrorStream(); //若有错误信息则输出
      if (proc == 0) {
        System.out.println("执行成功");
      } else {
        System.out.println("执行失败" + errStr);
      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }


  /**
   * 消费inputstream，并返回
   */
  public static String consumeInputStream(InputStream is) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(is, "GBK"));
    String s;
    StringBuilder sb = new StringBuilder();
    while ((s = br.readLine()) != null) {
      System.out.println(s);
      sb.append(s);
    }
    return sb.toString();
  }

  //这个方法比第一个好，执行时不会卡  stmt要执行的命令
  public static void aaa(String stam){
    BufferedReader br = null;
    try {
      File file = new File("D:\\daemonTmp");
      File tmpFile = new File("D:\\daemonTmp\\temp.tmp");//新建一个用来存储结果的缓存文件
      if (!file.exists()){
        file.mkdirs();
      }
      if(!tmpFile.exists()) {
        tmpFile.createNewFile();
      }
      ProcessBuilder pb = new ProcessBuilder().command("cmd.exe", "/c", stam).inheritIO();
      pb.redirectErrorStream(true);//这里是把控制台中的红字变成了黑字，用通常的方法其实获取不到，控制台的结果是pb.start()方法内部输出的。
      pb.redirectOutput(tmpFile);//把执行结果输出。
      pb.start().waitFor();//等待语句执行完成，否则可能会读不到结果。
      InputStream in = new FileInputStream(tmpFile);
      br= new BufferedReader(new InputStreamReader(in));
      String line = null;
      while((line = br.readLine()) != null) {
        System.out.println(line);
      }
      br.close();
      br = null;
      tmpFile.delete();//卸磨杀驴。
      System.out.println("执行完成");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if(br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
  /**
   * 清空脚本缓存流类，防止线程阻塞
   *
   *
   */
  static class ProcessClearStream extends Thread{
    private InputStream inputStream;
    private String type;

    public ProcessClearStream(InputStream inputStream, String type) {
      this.inputStream = inputStream;
      this.type = type;
    }

    @Override
    public void run() {
      try {
        InputStreamReader inputStreamReader = new InputStreamReader(
            inputStream,"GBK");
        BufferedReader br = new BufferedReader(inputStreamReader);
        // 打印信息
        String line = null;
        while ((line = br.readLine()) != null) {
          //LogUtils.debug(line);
          System.out.println(line);
        }
        // 不打印信息
//                 while (br.readLine() != null);
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }

}
