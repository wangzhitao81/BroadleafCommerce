package org.broadleafcommerce.openadmin.server.service.artifact.image;

import org.broadleafcommerce.openadmin.server.service.artifact.ArtifactProcessor;
import org.broadleafcommerce.openadmin.server.service.artifact.image.effects.chain.EffectsManager;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.*;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jfischer
 * Date: 9/10/11
 * Time: 11:58 AM
 * To change this template use File | Settings | File Templates.
 */
@Service("blImageArtifactProcessor")
public class ImageArtifactProcessor implements ArtifactProcessor {

    @Resource(name="blImageEffectsManager")
    protected EffectsManager effectsManager;

    protected String[] supportedUploadTypes = {"gif", "jpg", "jpeg", "png", "bmp", "wbmp"};
    protected float compressionQuality = 0.9F;

    @Override
    public boolean isSupported(InputStream artifactStream, String filename) {
        for (String type : supportedUploadTypes) {
            if (filename.endsWith(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Operation[] buildOperations(Map<String, String[]> parameterMap, InputStream artifactStream, String mimeType) {
        return effectsManager.buildOperations(parameterMap, artifactStream, mimeType);
    }

    public ImageMetadata getImageMetadata(InputStream artifactStream) throws Exception {
        ImageMetadata imageMetadata = new ImageMetadata();
        ImageInputStream iis = ImageIO.createImageInputStream(artifactStream);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (readers.hasNext()) {
            ImageReader reader = readers.next();
            reader.setInput(iis, true);
            imageMetadata.setWidth(reader.getWidth(0));
            imageMetadata.setHeight(reader.getHeight(0));
        } else {
            throw new Exception("Unable to retrieve image metadata from stream. Are you sure the stream provided is a valid input stream for an image source?");
        }

		return imageMetadata;
	}

    @Override
    public InputStream convert(InputStream artifactStream, Operation[] operations, String mimeType) throws Exception {
        Iterator<ImageReader> iter = ImageIO.getImageReaders(ImageIO.createImageInputStream(artifactStream));
        ImageReader reader = iter.next();
        String formatName = reader.getFormatName();

        BufferedImage image = ImageIO.read(ImageIO.createImageInputStream(artifactStream));

        //before
        if (formatName.equals("jpeg") || formatName.equals("jpg")) {
            image = stripAlpha(image);
        }

        for (Operation operation : operations){
            image = effectsManager.renderEffect(operation.getName(), operation.getFactor(), operation.getParameters(), image);
        }

        //and after - some applications have a problem reading jpeg images with an alpha channel associated
        if (formatName.equals("jpeg") || formatName.equals("jpg")) {
            image = stripAlpha(image);
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(byteArrayOutputStream);
        Iterator<ImageWriter> writerIter = ImageIO.getImageWritersByFormatName(formatName);
        ImageWriter writer = (ImageWriter) writerIter.next();
        ImageWriteParam iwp = writer.getDefaultWriteParam();
        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwp.setCompressionQuality(compressionQuality);
        MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(bos);
        writer.setOutput(output);

        IIOImage iomage = new IIOImage(image, null,null);
        writer.write(null, iomage, iwp);
        bos.flush();

        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    protected BufferedImage stripAlpha(BufferedImage image){
		BufferedImage raw_image=image;
		image = new BufferedImage(raw_image.getWidth(), raw_image.getHeight(), BufferedImage.TYPE_INT_RGB);
		ColorConvertOp xformOp=new ColorConvertOp(null);
		xformOp.filter(raw_image, image);

		return image;
	}

    public String[] getSupportedUploadTypes() {
        return supportedUploadTypes;
    }

    public void setSupportedUploadTypes(String[] supportedUploadTypes) {
        this.supportedUploadTypes = supportedUploadTypes;
    }

    public float getCompressionQuality() {
        return compressionQuality;
    }

    public void setCompressionQuality(float compressionQuality) {
        this.compressionQuality = compressionQuality;
    }
}
