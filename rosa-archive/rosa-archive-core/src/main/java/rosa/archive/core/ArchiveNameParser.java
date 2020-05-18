package rosa.archive.core;

import org.apache.commons.lang3.StringUtils;
import rosa.archive.model.ArchiveItemType;
import rosa.archive.model.BookImageLocation;
import rosa.archive.model.BookImageRole;


public class ArchiveNameParser implements ArchiveConstants {
//    private static final String DEFAULT_PAGE_REGEX = "([a-zA-Z]*)(\\d+)(r|v|R|V)";
    private static final String DEFAULT_PAGE_REGEX = "(([a-zA-Z]*)(\\d+)([rRvV]))|(\\d+)";
    private static final String DEFAULT_DELIMITER = "\\.";

    private final String delimiter;
    private final String page_regex;

    /**
     * Create an ArchiveNameParser with custom settings.
     *
     * @param page_regex regular expressions identifying pagination
     * @param delimiter string used to delimit segments of an archive item name
     */
    public ArchiveNameParser(String page_regex, String delimiter) {
        this.page_regex = page_regex == null ? DEFAULT_PAGE_REGEX : page_regex;
        this.delimiter = delimiter == null ? DEFAULT_DELIMITER : delimiter;
    }

    /**
     * Create an ArchiveNapeParser with default settings.
     */
    public ArchiveNameParser() {
        this(null, null);
    }

    /**
     * Get the location of the book image from its archive ID. If an unknown name
     * is encountered, NULL is returned.
     *
     * @param imageId ID of image in archive
     * @return the location of the book image
     */
    public BookImageLocation location(String imageId) {
        String[] parts = split_name(imageId);

        if (parts.length < 3) {
            return null;
        }

        String location = parts[1];
        for (BookImageLocation loc : BookImageLocation.values()) {
            if (loc.getInArchiveName().equals(location)) {
                return loc;
            }
        }

        if (location.matches(page_regex) && (parts.length == 3 || role(imageId) == BookImageRole.INSERT)) {
            return BookImageLocation.BODY_MATTER;
        }

        return null;
    }

    /**
     * Get the role of the book image from its archive ID.
     *
     * @param imageId ID of image in archive
     * @return the role of the book image
     */
    public BookImageRole role(String imageId) {
        String[] parts = split_name(imageId);
        
        for (int i = 1; i < parts.length - 1; i++) {
        	for (BookImageRole r : BookImageRole.values()) {
        		if (r.getArchiveName().equals(parts[i])) {
        			return r;
                }
        	}
        }

        return null;
    }

    /**
     * Get a short name, a human readable label.
     * The name may not be unique within the book.
     *
     * @param imageId ID of image in archive
     * @return short name
     */
    public String shortName(String imageId) {
        BookImageRole role = role(imageId);
        BookImageLocation location = location(imageId);
        String page = page(imageId);
        String insertNum = insertNumber(imageId);

        StringBuilder short_name = new StringBuilder();

        if (location != null) {
            short_name.append(location.getDisplay()).append(' ');
        }
        
        // Strip leading zeros from page number
        if (page != null) {
            short_name.append(page.replaceFirst("^0+(?!$)", "")).append(" ");
        }
        
        if (role != null) {
            short_name.append(role.getDisplay()).append(' ');
        }

        if (insertNum != null) {
            short_name.append(insertNum);
        }

        return short_name.toString().trim();
    }

    /**
     * Get the page number associated with an item in the archive. If the
     * item is associated with a part of the book that does not have a
     * page number (ex: front cover), NULL will be returned.
     *
     * @param imageId ID of item in archive
     * @return the page associated with the item
     */
    public String page(String imageId) {
        String[] parts = split_name(imageId);

        for (String part : parts) {
            if (part.matches(page_regex)) {
                return part;
            }
        }

        return null;
    }

    /**
     * There may be multiple images associated with an INSERT on a page. Get the image number.
     *
     * @param imageId ID of item in archive
     * @return image number for a particular insert, NULL there is no such number
     */
    public String insertNumber(String imageId) {
        String[] parts = imageId.split(delimiter);

        // Check for INSERT keyword, then return the following part if it is a number
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals(BookImageRole.INSERT.getArchiveName())) {
                String possible = parts[i+1];
                if (StringUtils.isNumeric(possible)) {
                    return possible;
                }
            }
        }

        return null;
    }

    /**
     * Does this name not have an associated image in the archive (is it missing)?
     *
     * @param imageId ID of image in archive
     * @return is the image missing?
     */
    public boolean isMissing(String imageId) {
        return imageId.startsWith(MISSING_PREFIX);
    }

    /**
     * Get the item type for an object in the archive from its name.
     *
     * @param name name if item in archive
     * @return item type
     */
    public ArchiveItemType getArchiveItemType(String name) {
        if (name.startsWith(".")) {
            return null;
        }

        if (name.trim().endsWith(TIF_EXT)) {
            return ArchiveItemType.IMAGE;
        }

        for (ArchiveItemType type : ArchiveItemType.values()) {
            if (name.contains(type.getIdentifier()) && name.trim().endsWith(type.getFileExtension())) {
                return type;
            }
        }

        return null;
    }

    private String[] split_name(String name) {
        return name.split(delimiter);
    }
    
    /**
     * 
     * @param imageId
     * @return identifier for image unique within book
     */
    public String shortUniqueImageIdInBook(String imageId) {
    	String[] parts = split_name(imageId);
    	
    	StringBuilder result = new StringBuilder();
    	for (int i = 1; i < parts.length - 1; i++) {
    		result.append(parts[i]);
    		
    		if (i != parts.length -2) {
    			result.append('.');
    		}
    	}
    	return result.toString();
    }
}
