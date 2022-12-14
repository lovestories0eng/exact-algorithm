package common;

import models.MetaPath;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author fangyixiang
 * @date 6 Nov. 2018
 * find a set S of vertices, which are linked with q
 * the main technique is batch-search with labelling (BSL)
 */
public class BatchLinker {
	private int[][] graph = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int[] vertexType = null;//vertex -> type
	private int[] edgeType = null;//edge -> type
	private List<Set<Integer>> labelList = null;//label a vertex if it participates in a meta-path  如果顶点参与元路径，则将其标记
	
	public BatchLinker(int[][] graph, int[] vertexType, int[] edgeType) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
	}
	
	public Set<Integer> link(int queryId, MetaPath metaPath){
		this.labelList = new ArrayList<Set<Integer>>();
		
		for(int i = 0;i < metaPath.pathLen + 1;i ++) {
			labelList.add(new HashSet<Integer>());
		}
		
		Set<Integer> rsSet = new HashSet<Integer>();
		Set<Integer> startSet = new HashSet<Integer>();
		rsSet.add(queryId);
		startSet.add(queryId);
		
		while(startSet.size() > 0) {
			Set<Integer> nextStartSet = label(startSet, metaPath);

			startSet = new HashSet<Integer>();
			for(int id:nextStartSet) {
				if(!rsSet.contains(id)) startSet.add(id);
				rsSet.add(id);
			}
		}
		
		return rsSet;
	}

	//带标签的查询
	private Set<Integer> label(Set<Integer> startSet, MetaPath metaPath) {
		int pathLen = metaPath.pathLen;
		
		//label the first layer
		Set<Integer> set0 = labelList.get(0);
		set0.addAll(startSet);
		
		//label the rest layers
		Set<Integer> batchSet = startSet;
		for(int index = 0;index < pathLen;index ++) {
			Set<Integer> nextLabelSet = labelList.get(index + 1);
			
			int targetVType = metaPath.vertex[index + 1], targetEType = metaPath.edge[index];
			Set<Integer> nextBatchSet = new HashSet<Integer>();
			for(int anchorId:batchSet) {
				int[] nbArr = graph[anchorId];
				for(int i = 0;i < nbArr.length;i += 2) {
					int nbVertexID = nbArr[i], nbEdgeID = nbArr[i + 1];
					if(targetVType == vertexType[nbVertexID] && targetEType == edgeType[nbEdgeID]) {
						if(nextLabelSet.contains(nbVertexID)) {
							//do nothing
						}else {
							if(index == metaPath.pathLen - 1) {//impose restriction
								nextBatchSet.add(nbVertexID);
							}else {
								nextBatchSet.add(nbVertexID);
							}
						}
						
//						System.out.println("BatchLinker: " + nbVertexID + " its type: " + vertexType[nbVertexID]);
					}
				}
			}

			nextLabelSet.addAll(nextBatchSet);
			
			batchSet = nextBatchSet;
		}
		
		return batchSet;
	}
}
