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
    // <edge id -> ArrayList<path>>
    HashMap<Integer, ArrayList<ArrayList<Integer>>> edgeMapPath;
    // 存储通过一跳搜索找到的新的节点
    Set<Integer> newFoundVertex = new HashSet<>();
    // 存储上一次一跳搜索找到的点
    Set<Integer> foundVertex = new HashSet<>();
    int pathLen = 0;
    double globalVertexPairConflict;
    double globalPathConflict;

    public QueryNodeExpandStrategy(int[][] graph, int[] vertexType, int[] edgeType, int[] edgeUsedTimes, HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge) {
        this.graph = graph;
        this.vertexType = vertexType;
        this.edgeType = edgeType;
        this.edgeUsedTimes = edgeUsedTimes;
        this.vertexPairMapEdge = vertexPairMapEdge;
    }

    public int[][] query(int queryId, MetaPath metaPath) {
        this.pathLen = metaPath.pathLen + 1;
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

        long startTime, endTime;
        while (true) {
            // step 2: expand the graph from the new-found node, find possibly linked vertex
            newFoundVertex = this.oneHopTraverse(foundVertex, metaPath, true);
            // 如果找不到新的点则结束
            if (newFoundVertex.size() == 0) break;

            // 根据树结构得到所有路径
            createPathSet(foundVertex, pathRecordMap);

            // step 3: link edges between new-found vertex set and original vertex set according to the pair conflict rule
            pathMapConflict = new HashMap<>();
            for (ArrayList<Integer> path : allPaths) {
                double conflict = calcPathConflict(path);
                // -1代表此条路径已有边的共享次数等于0
                if (conflict != -1)
                    pathMapConflict.put(path, calcPathConflict(path));
            }

            this.initVertexPairMap(true);

            // link edges with the lowest conflict utils vertex pairs are linked or no edges exists
            foundVertex = new HashSet<>();
            while (vertexPairMapConflict.size() != 0) {
                Map.Entry<Integer, Integer> tmp = null;
                double minConflict = Double.POSITIVE_INFINITY;
                int count = 0;
                // 找出冲突最小的点对，同时如果遍历完HashMap之后发现没有点对的冲突值等于globalVertexPairConflict则更新globalVertexPairConflict
                for (Map.Entry<Map.Entry<Integer, Integer>, Double> entry : vertexPairMapConflict.entrySet()) {
                    if (entry.getValue() < minConflict) {
                        tmp = entry.getKey();
                        minConflict = entry.getValue();
                    }

                    count++;

                    if (count == vertexPairMapConflict.size() && !(minConflict - globalVertexPairConflict < 1e-4)) {
                        globalVertexPairConflict = minConflict;
                    }

                    // 如果已经找到了全局最小冲突值则跳出循环
                    if (minConflict == globalVertexPairConflict) break;
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

            this.initVertexPairMap(true);

            while (vertexPairMapConflict.size() != 0) {
                Map.Entry<Integer, Integer> tmp = null;
                double minConflict = Double.POSITIVE_INFINITY;
                int count = 0;
                // 找出冲突最小的点对，同时如果遍历完HashMap之后发现没有点对的冲突值等于globalVertexPairConflict则更新globalVertexPairConflict
                for (Map.Entry<Map.Entry<Integer, Integer>, Double> entry : vertexPairMapConflict.entrySet()) {
                    if (entry.getValue() < minConflict) {
                        tmp = entry.getKey();
                        minConflict = entry.getValue();
                    }

                    count++;

                    if (count == vertexPairMapConflict.size() && !(minConflict - globalVertexPairConflict < 1e-4)) {
                        globalVertexPairConflict = minConflict;
                    }

                    // 如果已经找到了全局最小冲突值则跳出循环
                    if (minConflict == globalVertexPairConflict) break;
                }
                ArrayList<Integer> path = vertexPairMapPath.get(tmp);
                // 把发现的点加入”已找到点“集合中
                vertexFound.add(path.get(path.size() - 1));
                foundVertex.add(path.get(path.size() - 1));
                homeGraphBuilder(path, false);
                // update path conflict and vertex pair conflict
                updateVertexPairMap(path, false);
            }

            // // 为什么pathMapConflict的大小会增加？
            // while (pathMapConflict.size() > 0) {
            //     // 找到冲突值最小的路径
            //     ArrayList<Integer> tmpPath = new ArrayList<>();
            //     double minConflict = Double.POSITIVE_INFINITY;
            //     Map.Entry<ArrayList<Integer>, Double> entry;
            //     // TODO: optimization -> find the path with minimum conflict use step strategy, if path conflict equals 0, then when we meet a path with zero conflict, we break the loop
            //     // TODO: find the path with minimum conflict in O(1) time
            //      // startTime = System.currentTimeMillis();
            //     Iterator<Map.Entry<ArrayList<Integer>, Double>> iterator = pathMapConflict.entrySet().iterator();
            //     while (iterator.hasNext()) {
            //         entry = iterator.next();
            //         double conflict = entry.getValue();
            //         // 如果此条路径不可用则删除路径
            //         if (conflict == -1) {
            //             iterator.remove();
            //             continue;
            //         }
            //
            //         if (conflict < minConflict) {
            //             tmpPath = entry.getKey();
            //             minConflict = conflict;
            //         }
            //
            //         if (minConflict == globalPathConflict) break;
            //     }
            //
            //     // 如果遍历完HashMap都没找到全局冲突最小值，则更新全局冲突最小值
            //     if (!iterator.hasNext()) {
            //         globalPathConflict = minConflict;
            //     }
            //
            //     // endTime = System.currentTimeMillis();
            //     // System.out.println("loop");
            //     // System.out.println("运行时间：" + (endTime - startTime) + "ms");
            //     // System.out.println("Break point!");
            //
            //     if (minConflict <= -1) continue;
            //     if (tmpPath.size() == 0) continue;
            //
            //     // 如果该路径所代表的边在同构图中已经有了则跳过并且删除pathMapConflict中所有与此点对有关的边
            //     Set<Integer> linkVertexSet = homoGraph.get(tmpPath.get(0));
            //     int k = tmpPath.get(0);
            //     int v = tmpPath.get(tmpPath.size() - 1);
            //
            //     if (linkVertexSet.contains(tmpPath.get(tmpPath.size() - 1))) {
            //         // startTime = System.currentTimeMillis();
            //         updatePathMapConflict(k, v);
            //         // endTime = System.currentTimeMillis();
            //         // System.out.println("运行时间：" + (endTime - startTime) + "ms");
            //         // System.out.println("Break point!");
            //     } else {
            //         // startTime = System.currentTimeMillis();
            //         homeGraphBuilder(tmpPath, true);
            //         // endTime = System.currentTimeMillis();
            //         // System.out.println(1);
            //         // System.out.println("运行时间：" + (endTime - startTime) + "ms");
            //
            //         // startTime = System.currentTimeMillis();
            //         updateVertexPairMap(tmpPath, true);
            //         // endTime = System.currentTimeMillis();
            //         // System.out.println(2);
            //         // System.out.println("运行时间：" + (endTime - startTime) + "ms");
            //
            //         // startTime = System.currentTimeMillis();
            //         updatePathMapConflict(k, v);
            //         // endTime = System.currentTimeMillis();
            //         // System.out.println(3);
            //         // System.out.println("运行时间：" + (endTime - startTime) + "ms");
            //         // System.out.println("Break point!");
            //     }
            //     // System.out.println("Break point!");
            //
            // }
            System.out.println("Break point!");
        }

        System.out.println("Break point!");
        return null;
    }

    private void updatePathMapConflict(int k, int v) {
        ArrayList<ArrayList<Integer>> tmpPathRecorder = vertexPairMapAllPath.get(Map.entry(k, v));
        if (tmpPathRecorder != null) {
            for (ArrayList<Integer> path : tmpPathRecorder) {
                pathMapConflict.remove(path);
            }
            // vertexPairMapAllPath.remove(Map.entry(k, v));
        }

        tmpPathRecorder = vertexPairMapAllPath.get(Map.entry(v, k));
        if (tmpPathRecorder != null) {
            for (ArrayList<Integer> path : tmpPathRecorder) {
                pathMapConflict.remove(path);
            }
            // vertexPairMapAllPath.remove(Map.entry(v, k));
        }
    }

    private void initVertexPairMap(boolean mark) {
        if (mark) {
            vertexPairMapConflict = new HashMap<>();
            vertexPairMapPath = new HashMap<>();
            globalVertexPairConflict = Double.POSITIVE_INFINITY;

        }
        vertexPairMapAllPath = new HashMap<>();
        globalPathConflict = Double.POSITIVE_INFINITY;

        for (Map.Entry<ArrayList<Integer>, Double> entry : pathMapConflict.entrySet()) {
            ArrayList<Integer> key = entry.getKey();
            double conflict = entry.getValue();
            if (conflict < globalPathConflict) {
                globalPathConflict = conflict;
            }
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

                if (mark) {
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

                    if (conflict < globalVertexPairConflict) {
                        globalVertexPairConflict = conflict;
                    }
                }
            }
        }
    }

    /**
     * flag: 0 -> vertex pair, 1 -> path
     **/
    // TODO: optimization -> do not judge whether homoGraph contains some key, just get it, if it's null, then create a new set --- Done
    private void homeGraphBuilder(ArrayList<Integer> path, boolean flag) {
        Set<Integer> set = homoGraph.get(path.get(0));
        if (set != null) {
            set.add(path.get(path.size() - 1));
        } else {
            Set<Integer> newSet = new HashSet<>();
            newSet.add(path.get(path.size() - 1));
            homoGraph.put(path.get(0), newSet);
        }

        set = homoGraph.get(path.get(path.size() - 1));
        if (set != null) {
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
            // 优化：
            // vertexPairMapAllPath.remove(Map.entry(k, v));
        } else {
            pathMapConflict.remove(path);
            allPaths.remove(path);
            // // 删除有边容量小于等于0的路径 --- 似乎多余
            // Map.Entry<ArrayList<Integer>, Double> entry;
            // Iterator<Map.Entry<ArrayList<Integer>, Double>> iterator = pathMapConflict.entrySet().iterator();
            // while (iterator.hasNext()) {
            //     entry = iterator.next();
            //     ArrayList<Integer> tmpPath = entry.getKey();
            //     if (calcPathConflict(tmpPath) == -1) {
            //         iterator.remove();
            //     }
            // }
        }
    }

    /**
     * flag: 0 -> vertex pair, 1 -> path
     **/
    private void updateVertexPairMap(ArrayList<Integer> path, boolean flag) {
        // TODO: 使用edgeId或者vertexPair寻找到受影响的路径和点对以此提高速度 --- Done

        for (int i = 0; i < path.size() - 1; i++) {
            int k = path.get(i);
            int v = path.get(i + 1);
            int edgeId = vertexPairMapEdge.get(Map.entry(k, v));
            ArrayList<ArrayList<Integer>> tmpVar = edgeMapPath.get(edgeId);

            for (int j = 0; j < tmpVar.size(); j++) {
                ArrayList<Integer> integers = tmpVar.get(j);
                // 更新边冲突
                double conflict = calcPathConflict(integers);
                if (pathMapConflict.containsKey(integers))
                    pathMapConflict.put(integers, conflict);
                // 更新点对冲突
                if (!flag) {
                    int vertexPairStart = integers.get(0);
                    int vertexPairEnd = integers.get(integers.size() - 1);
                    Map.Entry<Integer, Integer> vertexPair = Map.entry(vertexPairStart, vertexPairEnd);
                    // 如果点对已经被更新过了则跳过
                    if (!vertexPairMapConflict.containsKey(vertexPair))
                        continue;
                    double oldConflict = vertexPairMapConflict.get(vertexPair);
                    double newConflict = conflict;
                    if (newConflict == -1) {
                        ArrayList<ArrayList<Integer>> tmp = vertexPairMapAllPath.get(vertexPair);
                        tmp.remove(integers);
                        tmpVar.remove(integers);
                        // 防止索引越界
                        j--;
                        // 如果此点对之间已无路径了则删除点对的键值，直接进行下一轮循环
                        if (tmp.size() == 0) {
                            vertexPairMapConflict.remove(vertexPair);
                            vertexPairMapPath.remove(vertexPair);
                            vertexPairMapAllPath.remove(vertexPair);
                            continue;
                        }
                        // 如果恰巧这条路径就是当前vertexMapPath中的路径
                        if (integers == vertexPairMapPath.get(vertexPair)) {
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
                        vertexPairMapPath.put(vertexPair, integers);
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

    /**
     * flag: to indicate whether we find edges from unknown vertex or known vertex
     **/
    private Set<Integer> oneHopTraverse(Set<Integer> startSet, MetaPath metaPath, boolean flag) {
        // TODO: Optimization need, try to filter some vertex
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
        // TODO: 减少生成路径集合的大小
        // 重新初始化所有路径集合
        allPaths = new ArrayList<>();
        // TODO: 选择边的Id来记录受影响的路径还是选择vertexPair来记录受影响的路径 --- Done
        // 选择edgeId: 需要先利用边获得点对，然后映射到edgeId以获得边的值，这样选择忽视了vertexPairMapEdge中的信息
        // 选择vertexPair，直接利用vertexPairMapEdge中的信息，但多了一步从vertexPair映射到edgeId的步骤
        // 先选择edgeId，关注时间
        // 重新初始化边对应的受影响路径
        edgeMapPath = new HashMap<>();
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

                if ((pathRecordMap.get(tempVertex) == null || reached.contains(tempVertex))) {
                    if (Objects.requireNonNull(tempPath).size() == pathLen) {
                        allPaths.add(tempPath);
                        // 遍历路径的所有边，在HashMap中以边为键映射到边
                        for (int i = 0; i < tempPath.size() - 1; i++) {
                            int k = tempPath.get(i);
                            int v = tempPath.get(i + 1);
                            int edgeId = vertexPairMapEdge.get(Map.entry(k, v));
                            if (edgeMapPath.containsKey(edgeId)) {
                                ArrayList<ArrayList<Integer>> tmpPathList = edgeMapPath.get(edgeId);
                                tmpPathList.add(tempPath);
                            } else {
                                ArrayList<ArrayList<Integer>> tempPathList = new ArrayList<>();
                                tempPathList.add(tempPath);
                                edgeMapPath.put(edgeId, tempPathList);
                            }
                        }
                    }
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
