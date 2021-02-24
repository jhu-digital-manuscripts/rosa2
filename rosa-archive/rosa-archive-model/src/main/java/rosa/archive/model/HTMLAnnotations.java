package rosa.archive.model;

import java.util.HashMap;
import java.util.Map;

/**
 * A set of HTML annotations that target books, images, or collections.
 * The HTML should be a simple and able to be embedded in an existing HTML document easily.
 */
public class HTMLAnnotations implements HasId {
	private final Map<String, String> annotations;
	private String id;
	
	public HTMLAnnotations() {
		this.annotations = new HashMap<String, String>();
	}
	
	public void setAnnotation(String target_id, String html) {
		annotations.put(target_id, html);
	}
	
	/**
	 * @param target_id
	 * @return HTML annotation with given target.
	 */
	public String getAnnotation(String target_id) {
		return annotations.get(target_id);
	}

	@Override
	public String getId() {
		return id;
	}


	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HTMLAnnotations other = (HTMLAnnotations) obj;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public int size() {
		return annotations.size();
	}
}
