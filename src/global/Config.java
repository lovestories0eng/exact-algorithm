package global;

public class Config {
    public static String root = "src";

    //DBLP
    // public static String dblpRoot = root + "\\DBLP_test\\";
    public static String dblpRoot = root + "\\DBLP\\";
    public static String dblpGraph = dblpRoot + "graph.txt";
    public static String dblpVertex = dblpRoot + "vertex.txt";
    public static String dblpEdge = dblpRoot + "edge.txt";
    // 查询节点ID
    // public static int queryNodeId = 0;
    public static int queryNodeId = 552985;
    // public static int queryNodeId = 2714;
    // k值
    // SHARED_TIME = 3
    // public static int k = 30;
    // SHARED_TIME = 2
    public static int k = 65;
    // SHARED_TIME = 1
    // public static int k = 61;
    // public static int k = 3;

    // 点的共享次数
    public static int SHARED_TIMES = 2;
    // 元路径
    public static String[] META_PATH = new String[]{"A", "P", "A", "sink"};
    // 元路径长度
    public static int META_PATH_LENGTH = 3;
    // 文件路径
    public static String FILE_PATH = "src/data";


}
