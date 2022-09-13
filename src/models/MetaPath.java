package models;

/**
 * @author
 * @date
 * 
 * A meta-path with (pathLen + 1) vertices and pathLen edges
 */
public class MetaPath {
	public int[] vertex;
	public int[] edge;
	public int pathLen = -1;
	
	public MetaPath(int[] vertex, int[] edge) {
		this.vertex = vertex;
		this.edge = edge;
		this.pathLen = edge.length;//the number of relations in a meta-path
		
		if(vertex.length != edge.length + 1) {
			System.out.println("the meta-path is incorrect");
		}
	}
	
	public MetaPath(String metaPathStr) {
		String[] s = metaPathStr.trim().split(" ");
		this.pathLen = s.length / 2;
		this.vertex = new int[pathLen +1];
		this.edge = new int[pathLen];
		
		for(int i = 0;i < s.length;i ++) {
			int value = Integer.parseInt(s[i]);
			if(i % 2 == 0) {
				vertex[i / 2] = value;
			}else {
				edge[i / 2] = value;
			}
		}
	}
	
	public String toString() {
		String str = "";
		for(int i = 0;i < pathLen;i ++) {
			str += vertex[i] + "-" + edge[i] + "-";
		}
		str += vertex[pathLen];
		return str;
	}
}
