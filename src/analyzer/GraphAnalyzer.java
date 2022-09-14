package analyzer;

import global.Config;
import methods.DataReader;


public class GraphAnalyzer {
    /**
     * check whether there is always a bidirectional edge between link nodes
     **/
    public static void main(String[] args) {
        DataReader dataReader = new DataReader(Config.dblpGraph, Config.dblpVertex, Config.dblpEdge);
        int[][] graph = dataReader.readGraph();
        int[] vertexType = dataReader.readVertexType();
        int[] edgeType = dataReader.readEdgeType();

        for (int i = 0; i < graph.length; i++) {
            for (int j = 0; j < graph[i].length; j += 2) {
                int vertexId = graph[i][j];
                int edgeId = graph[i][j + 1];
                boolean exist = false;
                for (int k = 0; k < graph[vertexId].length; k += 2) {
                    int vertexReverseId = graph[vertexId][k];
                    if (vertexReverseId == i) {
                        exist = true;
                        break;
                    }
                }
                if (!exist) {
                    System.out.println("not bidirectional");
                }
            }
        }

    }
}
