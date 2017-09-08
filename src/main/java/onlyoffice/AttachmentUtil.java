package onlyoffice;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.User;

public class AttachmentUtil
{
	private static final Logger log = LogManager.getLogger("onlyoffice.AttachmentUtil");

	public static boolean checkAccess(Long attachmentId, User user, boolean forEdit)
	{
		if (user == null)
		{
			return false;
		}

		AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
		Attachment attachment = attachmentManager.getAttachment(attachmentId);

		return checkAccess(attachment, user, forEdit);
	}

	public static boolean checkAccess(Attachment attachment, User user, boolean forEdit)
	{
		if (user == null)
		{
			return false;
		}

		PermissionManager permissionManager = (PermissionManager) ContainerManager.getComponent("permissionManager");

		Permission permission = Permission.VIEW;
		if (forEdit)
		{
			permission = Permission.EDIT;
		}

		boolean access = permissionManager.hasPermission(user, permission, attachment);
		return access;
	}

	public static void saveAttachment(Long attachmentId, InputStream attachmentData, int size, ConfluenceUser user)
			throws IOException, IllegalArgumentException
	{
		AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
		Attachment attachment = attachmentManager.getAttachment(attachmentId);
		
		Attachment oldAttachment = attachment.copy();
		attachment.setFileSize(size);

		AuthenticatedUserThreadLocal.set(user);

		attachmentManager.saveAttachment(attachment, oldAttachment, attachmentData);
	}

	public static InputStream getAttachmentData(Long attachmentId)
	{
		AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
		Attachment attachment = attachmentManager.getAttachment(attachmentId);
		return attachmentManager.getAttachmentData(attachment);
	}

	public static String getMediaType(Long attachmentId)
	{
		AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
		Attachment attachment = attachmentManager.getAttachment(attachmentId);
		return attachment.getMediaType();
	}

	public static String getFileName(Long attachmentId)
	{
		AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
		Attachment attachment = attachmentManager.getAttachment(attachmentId);
		return attachment.getFileName();
	}

	public static String getHashCode(Long attachmentId)
	{
		AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
		Attachment attachment = attachmentManager.getAttachment(attachmentId);
		int hashCode = attachment.hashCode();
		log.info("hashCode = " + hashCode);

		int version = attachment.getVersion();
		return attachmentId + "_" + version + "_" + hashCode;
	}
}