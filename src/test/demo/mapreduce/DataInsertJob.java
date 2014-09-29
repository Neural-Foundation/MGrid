package test.demo.mapreduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import test.demo.mapreduce.DataInsertMapper;;

public class DataInsertJob {

	public static void main(String[] args) throws Exception{
		System.setProperty("HADOOP_USER_NAME","hduser");	
		Configuration conf = new Configuration();
	//	conf.addResource(new Path("/home/hduser/vol01/hadoop/conf/core-site.xml"));
	//	conf.addResource(new Path("/home/hduser/vol01/hadoop/conf/hdfs-site.xml"));
	//	conf.set("mgrid.jar", "/home/hduser/vol01/mgrid2d.jar");
		Job job = new Job(conf,"DataInsertJob");
			job.setJarByClass(DataInsertMapper.class);
			Scan scan = new Scan();
			scan.setCaching(500);
			scan.setCacheBlocks(false);
			
			TableMapReduceUtil.initTableMapperJob("mgrid2dm", scan, 	DataInsertMapper.class, null, null, job);
			TableMapReduceUtil.initTableReducerJob("slave23.mst.edu", null, job);
			
			job.setNumReduceTasks(0);	
			
			//FileInputFormat.addInputPath(job, new Path("hdfs://master20.mst.edu:54310/home/hduser/vol01/movingdata/output-50m-all-settings.txt"));
		   // FileOutputFormat.setOutputPath(job, new Path( "/home/hduser/vol01/output/"));
			System.exit(job.waitForCompletion(true) ? 0 : 1);


	}

}
