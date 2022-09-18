package utils.impl;

import common.BatchLinker;
import global.Config;
import models.MetaPath;
import utils.InitialGraphConstructor;

import java.util.*;

// 从查询点扩展构建初始图
public class QueryNodeExpandStrategy implements InitialGraphConstructor {
    private int[][] graph;//data graph, including vertex IDs, edge IDs, and their link relationships
    private int[] vertexType;//vertex -> type
    private int[] edgeType;//edge -> type
    private int[] edgeUsedTimes;//edge -> used times
    private HashMap<Integer, Set<Integer>> homoGraph = new HashMap<>();
    // 记录找到的所有节点
    Set<Integer> vertexFound = new HashSet<>();
    HashMap<Integer, ArrayList<Integer>> pathRecordMap = new HashMap<>();
    Set<Integer> keepSet = null;
    HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge;
    // path conflict
    HashMap<ArrayList<Integer>, Double> pathMapConflict;
    // <vertex pair -> path with lowest conflict>
    HashMap<Map.Entry<Integer, Integer>, ArrayList<Integer>> vertexPairMapPath;
    // <vertex pair -> path conflict>
    HashMap<Map.Entry<Integer, Integer>, Double> vertexPairMapConflict;
    // TODO: 为每个点对存储所有的路径，从而在知道可能受影响的点对之后更新其冲突值与对应路径
    // <vertex pair -> all path between two vertex>
    HashMap<Map.Entry<Integer, Integer>, ArrayList<ArrayList<Integer>>> vertexPairMapAllPath;
    // 存储所有路径
    ArrayList<ArrayList<Integer>> allPaths = new ArrayList<>();
    // 存储通过一跳搜索找到的新的节点
    Set<Integer> newFoundVertex = new HashSet<>();
    // 存储上一次一跳搜索找到的点
    Set<Integer> foundVertex = new HashSet<>();

    public QueryNodeExpandStrategy(int[][] graph, int[] vertexType, int[] edgeType, int[] edgeUsedTimes, HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge) {
        this.graph = graph;
        this.vertexType = vertexType;
        this.edgeType = edgeType;
        this.edgeUsedTimes = edgeUsedTimes;
        this.vertexPairMapEdge = vertexPairMapEdge;
    }

    public int[][] query(int queryId, MetaPath metaPath) {
        if (metaPath.vertex[0] != vertexType[queryId]) {
            System.out.println("查询节点类型与元路径类型不匹配！");
            return null;
        }


        // 存储寻找到的所有节点
        vertexFound.add(queryId);
        foundVertex.add(queryId);

        // step 1: compute the connected subgraph via batch-search with labeling (BSL)
        BatchLinker batchLinker = new BatchLinker(graph, vertexType, edgeType);
        keepSet = batchLinker.link(queryId, metaPath);

        while (foundVertex.size() != 0) {
            // step 2: expand the graph from the new-found node, find possibly linked vertex
            newFoundVertex = this.oneHopTraverse(foundVertex, metaPath, true);

            // 根据树结构得到所有路径
            createPathSet(foundVertex, pathRecordMap);

            // step 3: link edges between new-found vertex set and original vertex set according to the pair conflict rule
            pathMapConflict = new HashMap<>();
            for (ArrayList<Integer> path : allPaths) {
                double conflict = calcPathConflict(path);
                if (conflict != -1)
                    pathMapConflict.put(path, calcPathConflict(path));
            }

            this.initVertexPairMap();

            // link edges with the lowest conflict utils or vertex pairs are linked or no edges exists
            foundVertex = new HashSet<>();
            while (vertexPairMapConflict.size() != 0) {
                Map.Entry<Integer, Integer> tmp = null;
                double conflict = Double.POSITIVE_INFINITY;
                // 找出冲突最小的点对
                for (Map.Entry<Map.Entry<Integer, Integer>, Double> entry : vertexPairMapConflict.entrySet()) {
                    if (entry.getValue() < conflict) {
                        tmp = entry.getKey();
                    }
                }
                ArrayList<Integer> path = vertexPairMapPath.get(tmp);
                // 把发现的点加入”已找到点“集合中
                vertexFound.add(path.get(path.size() - 1));
                foundVertex.add(path.get(path.size() - 1));
                homeGraphBuilder(path, false);
                // update path conflict and vertex pair conflict
                updateVertexPairMap(path, false);

            }


            // step 4: link edges inner the new-found vertex
            oneHopTraverse(foundVertex, metaPath, false);
            createPathSet(foundVertex, pathRecordMap);

            pathMapConflict = new HashMap<>();
            for (ArrayList<Integer> tmpPath : allPaths) {
                double conflict = calcPathConflict(tmpPath);
                if (conflict != -1)
                    pathMapConflict.put(tmpPath, conflict);
            }

            ArrayList<Integer> tmpPath = new ArrayList<>();
            double minConflict = Double.POSITIVE_INFINITY;

            while (pathMapConflict.size() > 0) {
                // 找到冲突值最小的路径
                Map.Entry<ArrayList<Integer>, Double> entry;
                for (Map.Entry<ArrayList<Integer>, Double> arrayListDoubleEntry : pathMapConflict.entrySet()) {
                    entry = arrayListDoubleEntry;
                    if (entry.getValue() < minConflict) {
                        tmpPath = entry.getKey();
                    }
                }
                // 如果该路径所代表的边在同构图中已经有了则跳过
                Set<Integer> linkVertexSet = homoGraph.get(tmpPath.get(0));
                if (linkVertexSet.contains(tmpPath.get(tmpPath.size() - 1))) {
                    pathMapConflict.remove(tmpPath);
                    continue;
                }
                homeGraphBuilder(tmpPath, true);
                updateVertexPairMap(tmpPath, true);
                // for (Integer integer : tmpPath) {
                //     System.out.printf("%d ", integer);
                // }
                // System.out.println();
            }
        }

        return null;
    }

    private void initVertexPairMap() {
        vertexPairMapConflict = new HashMap<>();
        vertexPairMapPath = new HashMap<>();
        vertexPairMapAllPath = new HashMap<>();

        for (Map.Entry<ArrayList<Integer>, Double> entry : pathMapConflict.entrySet()) {
            ArrayList<Integer> key = entry.getKey();
            double conflict = entry.getValue();
            int k = key.get(0);
            int v = key.get(key.size() - 1);
            // 判断边容量是否大于0
            boolean flag = true;
            for (int i = 0; i < key.size() - 1; i++) {
                int tmpK = key.get(i), tmpV = key.get(i + 1);
                if (edgeUsedTimes[vertexPairMapEdge.get(Map.entry(tmpK, tmpV))] <= 0) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                // 记录点对间的所有路径
                if (vertexPairMapAllPath.containsKey(Map.entry(k, v))) {
                    ArrayList<ArrayList<Integer>> tmp = vertexPairMapAllPath.get(Map.entry(k, v));
                    tmp.add(key);
                } else {
                    ArrayList<ArrayList<Integer>> tmp = new ArrayList<>();
                    tmp.add(key);
                    vertexPairMapAllPath.put(Map.entry(k, v), tmp);
                }

                // 记录点对间的最小冲突值
                if (vertexPairMapConflict.containsKey(Map.entry(k, v))) {
                    if (conflict < vertexPairMapConflict.get(Map.entry(k, v))) {
                        vertexPairMapConflict.put(Map.entry(k, v), conflict);
                        vertexPairMapPath.put(Map.entry(k, v), key);
                    }
                } else {
                    vertexPairMapConflict.put(Map.entry(k, v), conflict);
                    vertexPairMapPath.put(Map.entry(k, v), key);
                }
            }
        }
    }

    /**
     * flag: 0 -> vertex pair, 1 -> path
     **/
    private void homeGraphBuilder(ArrayList<Integer> path, boolean flag) {
        if (homoGraph.containsKey(path.get(0))) {
            Set<Integer> set = homoGraph.get(path.get(0));
            set.add(path.get(path.size() - 1));
        } else {
            Set<Integer> newSet = new HashSet<>();
            newSet.add(path.get(path.size() - 1));
            homoGraph.put(path.get(0), newSet);
        }

        if (homoGraph.containsKey(path.get(path.size() - 1))) {
            Set<Integer> set = homoGraph.get(path.get(path.size() - 1));
            set.add(path.get(0));
        } else {
            Set<Integer> newSet = new HashSet<>();
            newSet.add(path.get(0));
            homoGraph.put(path.get(path.size() - 1), newSet);
        }

        // 更新边容量
        for (int i = 0; i < path.size() - 1; i++) {
            edgeUsedTimes[vertexPairMapEdge.get(Map.entry(path.get(i), path.get(i + 1)))]--;
        }

        if (!flag) {
            // 如果点对已连接则去除其在HashMap中的键值对
            int k = path.get(0);
            int v = path.get(path.size() - 1);
            vertexPairMapConflict.remove(Map.entry(k, v));
            vertexPairMapPath.remove(Map.entry(k, v));
        } else {
            pathMapConflict.remove(path);
            allPaths.remove(path);
            // 删除有边容量小于等于0的路径
            Map.Entry<ArrayList<Integer>, Double> entry;
            Iterator<Map.Entry<ArrayList<Integer>, Double>> iterator = pathMapConflict.entrySet().iterator();
            while (iterator.hasNext()) {
                entry = iterator.next();
                ArrayList<Integer> tmpPath = entry.getKey();
                if (calcPathConflict(tmpPath)  == -1) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * flag: 0 -> vertex pair, 1 -> path
     **/
    private void updateVertexPairMap(ArrayList<Integer> path, boolean flag) {
        // 存储可能受影响的点对
        Set<Map.Entry<Integer, Integer>> affVertexPair = new HashSet<>();
        // 存储可能受影响的路径
        Set<ArrayList<Integer>> affPath = new HashSet<>();
        // 找出可能受影响的路径
        for (ArrayList<Integer> tmp : allPaths) {
            for (int j = 0; j < tmp.size() - 1; j++) {
                int k1 = tmp.get(j), v1 = tmp.get(j + 1);
                int k2 = path.get(j), v2 = path.get(j + 1);
                if (k1 == k2 && v1 == v2) {
                    pathMapConflict.put(tmp, calcPathConflict(tmp));
                    affPath.add(tmp);

                    if (!flag) {
                        int k = tmp.get(0), v = tmp.get(tmp.size() - 1);
                        if (vertexPairMapPath.containsKey(Map.entry(k, v))) {
                            affVertexPair.add(Map.entry(k, v));
                        }
                    }

                    break;
                }
            }
        }
        if (!flag) {
            // 提取受影响的点对
            for (Map.Entry<Integer, Integer> vertexPair : affVertexPair) {
                // 找到点对之间的所有路径
                ArrayList<ArrayList<Integer>> pathList = vertexPairMapAllPath.get(vertexPair);

                for (int i = 0; i < pathList.size(); i++) {
                    ArrayList<Integer> tmpPath = pathList.get(i);
                    // 如果此条边收到了影响
                    if (affPath.contains(tmpPath)) {
                        double oldConflict = vertexPairMapConflict.get(vertexPair);
                        double newConflict = calcPathConflict(tmpPath);
                        if (newConflict == -1) {
                            ArrayList<ArrayList<Integer>> tmp = vertexPairMapAllPath.get(vertexPair);
                            tmp.remove(tmpPath);
                            i--;
                            // 如果此点对之间已无路径了则删除点对的键值，直接进行下一轮循环
                            if (tmp.size() == 0) {
                                vertexPairMapConflict.remove(vertexPair);
                                vertexPairMapPath.remove(vertexPair);
                                vertexPairMapAllPath.remove(vertexPair);
                                break;
                            }
                            // 如果恰巧这条路径就是当前vertexMapPath中的路径
                            if (tmpPath == vertexPairMapPath.get(vertexPair)) {
                                Double tmpConflict = Double.POSITIVE_INFINITY;
                                // 另外找出一条冲突最小的路径
                                for (ArrayList<Integer> minPath : tmp) {
                                    Double anotherConflict = pathMapConflict.get(minPath);
                                    if (anotherConflict < tmpConflict) {
                                        vertexPairMapConflict.put(vertexPair, anotherConflict);
                                        vertexPairMapPath.put(vertexPair, minPath);
                                    }
                                }
                            }
                        } else if (newConflict < oldConflict) {
                            vertexPairMapConflict.put(vertexPair, newConflict);
                            vertexPairMapPath.put(vertexPair, tmpPath);
                        }
                    }
                }
            }
        }
    }

    private double calcPathConflict(ArrayList<Integer> path) {
        double totalTimes = Config.SHARED_TIMES;

        double usedTimesInTotal = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            usedTimesInTotal += totalTimes - edgeUsedTimes[vertexPairMapEdge.get(Map.entry(path.get(i), path.get(i + 1)))];
            // 如果有边的容量小于等于0
            if (edgeUsedTimes[vertexPairMapEdge.get(Map.entry(path.get(i), path.get(i + 1)))] <= 0) {
                // -1代表此条路径已经不可用
                return -1;
            }
        }
        return usedTimesInTotal / (totalTimes * (path.size() - 1));
    }

    // private Set<Integer> oneHopTraverse(Set<Integer> startSet, MetaPath metaPath) {
    //     int pathLen = metaPath.pathLen;
    //     Set<Integer> batchSet = startSet;
    //
    //     pathRecordMap = new HashMap<>();
    //
    //     for (int index = 0; index < pathLen; index++) {
    //         int targetVType = metaPath.vertex[index + 1], targetEType = metaPath.edge[index];
    //
    //         Set<Integer> nextBatchSet = new HashSet<>();
    //         for (int anchorId : batchSet) {
    //             int[] nbArr = graph[anchorId];
    //             for (int i = 0; i < nbArr.length; i += 2) {
    //                 int nbVertexId = nbArr[i], nbEdgeId = nbArr[i + 1];
    //                 if (targetVType == vertexType[nbVertexId]
    //                         && targetEType == edgeType[nbEdgeId]
    //                         && edgeUsedTimes[nbEdgeId] > 0
    //                         && !vertexFound.contains(nbVertexId)
    //                 ) {
    //                     if (index == metaPath.pathLen - 1) {
    //                         if (keepSet.contains(nbVertexId)) {
    //                             nextBatchSet.add(nbVertexId);
    //                             recordPath(pathRecordMap, anchorId, nbVertexId);
    //                         }
    //                     } else {
    //                         nextBatchSet.add(nbVertexId);
    //                         recordPath(pathRecordMap, anchorId, nbVertexId);
    //                     }
    //                 }
    //             }
    //         }
    //         batchSet = nextBatchSet;
    //     }
    //     return batchSet;
    // }

    /**
     * flag: to indicate whether we find edges from unknown vertex or known vertex
     **/
    private Set<Integer> oneHopTraverse(Set<Integer> startSet, MetaPath metaPath, boolean flag) {
        int pathLen = metaPath.pathLen;
        Set<Integer> batchSet = startSet;

        pathRecordMap = new HashMap<>();
        allPaths = new ArrayList<>();

        for (int index = 0; index < pathLen; index++) {
            int startPoint = 0;
            int targetVType = metaPath.vertex[index + 1], targetEType = metaPath.edge[index];

            Set<Integer> nextBatchSet = new HashSet<>();
            for (int anchorId : batchSet) {
                int[] nbArr = graph[anchorId];
                for (int i = 0; i < nbArr.length; i += 2) {
                    int nbVertexId = nbArr[i], nbEdgeId = nbArr[i + 1];
                    if (targetVType == vertexType[nbVertexId]
                            && targetEType == edgeType[nbEdgeId]
                            && edgeUsedTimes[nbEdgeId] > 0
                    ) {
                        // 记录起始点位置
                        if (index == 0) {
                            startPoint = anchorId;
                        }
                        if (index == metaPath.pathLen - 1) {
                            if (flag) {
                                // 目的是为了找到没有找到过的点，因此如果发现此点已经被找到则跳过
                                if (vertexFound.contains(nbVertexId))
                                    continue;
                            } else {
                                // 只寻找在已知图中的边且必须是新找到的点集合中的
                                if (!foundVertex.contains(nbVertexId) || nbVertexId == startPoint) {
                                    continue;
                                }
                                // 如果同构图中已经有边则跳过
                                Set<Integer> vertexList = homoGraph.get(nbVertexId);
                                if (vertexList.contains(anchorId)) {
                                    continue;
                                }
                            }
                            if (keepSet.contains(nbVertexId)) {
                                nextBatchSet.add(nbVertexId);
                                recordPath(pathRecordMap, anchorId, nbVertexId);
                            }
                        } else {
                            nextBatchSet.add(nbVertexId);
                            recordPath(pathRecordMap, anchorId, nbVertexId);
                        }


                    }
                }
            }
            batchSet = nextBatchSet;
        }

        return batchSet;
    }

    private void createPathSet(Set<Integer> vertexSet, HashMap<Integer, ArrayList<Integer>> pathRecordMap) {
        // 重新初始化所有路径集合
        allPaths = new ArrayList<>();
        for (int vertexStart : vertexSet) {
            // 记录已经到达的点
            Set<Integer> reached = new HashSet<>();
            // 存储从单个点开始的所有路径
            Queue<ArrayList<Integer>> path = new LinkedList<>();
            Queue<Integer> queue = new LinkedList<>();

            ArrayList<Integer> temp = new ArrayList<>();
            temp.add(vertexStart);
            path.offer(temp);
            queue.offer(vertexStart);
            while (!queue.isEmpty()) {
                int tempVertex = queue.poll();
                ArrayList<Integer> tempPath = path.poll();

                if (pathRecordMap.get(tempVertex) == null || reached.contains(tempVertex)) {
                    allPaths.add(tempPath);
                } else {
                    ArrayList<Integer> tmpArr = pathRecordMap.get(tempVertex);
                    for (Integer integer : tmpArr) {
                        if (integer != vertexStart) {
                            if (vertexType[integer] == vertexType[vertexStart])
                                reached.add(integer);
                            queue.offer(integer);
                            assert tempPath != null;
                            ArrayList<Integer> newArr = new ArrayList<>(tempPath);
                            newArr.add(integer);
                            path.offer(newArr);
                        }
                    }
                }
            }
        }

    }

    private void recordPath(HashMap<Integer, ArrayList<Integer>> pathRecordMap, int anchorId, int nbVertexId) {
        if (!pathRecordMap.containsKey(anchorId)) {
            ArrayList<Integer> vertexLinker = new ArrayList<>();
            vertexLinker.add(nbVertexId);
            pathRecordMap.put(anchorId, vertexLinker);
        } else {
            ArrayList<Integer> vertexLinkerPointer = pathRecordMap.get(anchorId);
            vertexLinkerPointer.add(nbVertexId);
        }
    }
}
