// C:\Users\Oz\idc\3rd Year semester b\גרפיקה ממוחשבת\Ex1\pictures\bench.png


package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {

	//MARK: Fields
	public final Logger logger;
	public final BufferedImage workingImage;
	public final RGBWeights rgbWeights;
	public final int inWidth;
	public final int inHeight;
	public final int workingImageType;
	public final int outWidth;
	public final int outHeight;

	//MARK: Constructors
	public ImageProcessor(Logger logger, BufferedImage workingImage,
			RGBWeights rgbWeights, int outWidth, int outHeight) {
		super(); //Initializing for each loops...

		this.logger = logger;
		this.workingImage = workingImage;
		this.rgbWeights = rgbWeights;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.outWidth = outWidth;
		this.outHeight = outHeight;
		setForEachInputParameters();
	}

	public ImageProcessor(Logger logger,
			BufferedImage workingImage,
			RGBWeights rgbWeights) {
		this(logger, workingImage, rgbWeights,
				workingImage.getWidth(), workingImage.getHeight());
	}

	//MARK: Change picture hue - example
	public BufferedImage changeHue() {
		logger.log("Prepareing for hue changing...");

		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;

		BufferedImage ans = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r*c.getRed() / max;
			int green = g*c.getGreen() / max;
			int blue = b*c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});

		logger.log("Changing hue done!");

		return ans;
	}


	//MARK: Unimplemented methods
	/**
	 * @return the grey scale image of the given working Image.
	 */
	public BufferedImage greyscale() {
		logger.log("Prepareing for greyscale changing...");

		int redWeight = rgbWeights.redWeight;
		int greenWeight = rgbWeights.greenWeight;
		int blueWeight = rgbWeights.blueWeight;

		BufferedImage ans = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color currentPixel = new Color(workingImage.getRGB(x, y));
			int newRed = redWeight*currentPixel.getRed();
			int newGreen = greenWeight*currentPixel.getGreen();
			int newBlue = blueWeight*currentPixel.getBlue();
			int sum = (newRed + newGreen + newBlue ) / (redWeight + greenWeight +blueWeight);
			Color newPixel = new Color(sum, sum, sum);
			ans.setRGB(x, y, newPixel.getRGB());
		});

		logger.log("Changing greyscale done!");

		return ans;
	}


	public BufferedImage gradientMagnitude() {
		logger.log("Prepareing for gradientMagnitude changing...");

		try {

			if(workingImage.getWidth()<2 || workingImage.getHeight()<2)
				throw new Exception("Image dimensions are too small");
		}
		catch (Exception e) {
			// TODO: print to logger
		}

		final BufferedImage ans = addZeros(greyscale());

		forEach((y, x) -> {
			if(( x>0 && x < ans.getWidth() - 1 ) && ( y>0 && y< ans.getHeight() - 1 ) )
			{

				Color currentPixel = new Color(ans.getRGB(x, y));
				Color prevHorizontalPixel = new Color(ans.getRGB(x-1, y));
				Color prevVerticalPixel = new Color(ans.getRGB(x, y-1));
				int currentGreyValue = currentPixel.getRed(); // Becausse all values are the same (greyScale)
				int prevHorizontalGreyValue = prevHorizontalPixel.getRed();
				int prevVerticalGreyValue = prevVerticalPixel.getRed();

				int dxSqr = (int) Math.pow((currentGreyValue - prevHorizontalGreyValue), 2);
				int dySqr = (int) Math.pow((currentGreyValue - prevVerticalGreyValue), 2);
				int currMagnitude = (int) Math.sqrt((dxSqr + dySqr)/2);
				Color newPixel = new Color(currMagnitude, currMagnitude, currMagnitude);
				ans.setRGB(x, y, newPixel.getRGB());
			}
		});

		logger.log("Changing gradientMagnitude done!");
		return ans;
	}

	/**
	 * nearestNeighbor returns a new BufferedImage accordingly to nearest neighbor method.
	 * @return the new image accordingly to the nearest neighbor method.
	 */
	public BufferedImage nearestNeighbor() {
		logger.log("Prepareing for nearestNeighbor changing...");
		
		BufferedImage ans = newEmptyImage(outWidth, outHeight);
		double x_ratio = inWidth/(double)outWidth ;
		double y_ratio = inHeight/(double)outHeight ;
		int origX, origY ; 
		for (int y=0;y<outHeight;y++) {
			for (int x=0;x<outWidth;x++) {
				origX = (int) Math.floor(x*x_ratio) ;
				origY = (int) Math.floor(y*y_ratio) ;
				ans.setRGB(x, y, workingImage.getRGB(origX,origY));
			}
		}
		
		logger.log("Changing nearestNeighbor done!");
		return ans ;
	}
    
	/**
	 * bilinear returns the new BufferedImage accordingly to the bilinear method taught in class.
	 * @return the new BufferedImage.
	 */
	public BufferedImage bilinear() {
		logger.log("Prepareing for bilinear changing...");
		BufferedImage tmp = addZeros(workingImage);
		BufferedImage ans = newEmptyImage(outWidth, outHeight);
		
		double scaleWidth = (double) outWidth / tmp.getWidth();
    	double scaleHeight = (double) outHeight / tmp.getHeight();
		
        for (int xNew = 1; xNew < outWidth - 2; xNew++) {
            for (int yNew = 1; yNew < outHeight - 2; yNew++) {
       
                float x0 = (float) ((xNew) / scaleWidth);
                float y0 = (float) (( yNew) / scaleHeight);
                
                int x1 = (int) Math.floor(x0);
                int x2 = (int) Math.ceil(x0);
                int y1 = (int) Math.floor(y0);
                int y2 = (int) Math.ceil(y0);
                
                Color SW = new Color(tmp.getRGB(x1, y1));
                Color NW = new Color(tmp.getRGB(x1 , y2));
                Color SE= new Color(tmp.getRGB(x2 , y1));
                Color NE= new Color(tmp.getRGB(x2 , y2));
                
                // dx = u, dy = v;
                float dx = x0 - x1;
                float dy = y0 - y1;
                float sRed = ( SE.getRed()*dx )+ ( SW.getRed()*(1-dx) );
                float nRed = ( NE.getRed()*dx )+ ( NW.getRed() * (1-dx));
                int vRed = (int) ( ( nRed * dy ) + ( sRed * (1-dy) ) );
                
                float sGreen = ( SE.getGreen()*dx )+ ( SW.getGreen()*(1-dx) );
                float nGreen = ( NE.getGreen()*dx )+ ( NW.getGreen() * (1-dx));
                int vGreen = (int) ( ( nGreen * dy ) + ( sGreen * (1-dy) ) );
                
                float sBlue = ( SE.getBlue()*dx )+ ( SW.getBlue()*(1-dx) );
                float nBlue = ( NE.getBlue()*dx )+ ( NW.getBlue() * (1-dx));
                int vBlue = (int) ( ( nBlue * dy ) + ( sBlue * (1-dy) ) );
                
                ans.setRGB(xNew, yNew, new Color(vRed,vGreen,vBlue).getRGB());
            }
        }
        logger.log("Changing bilinear done!");
        return ans;
	}



	//MARK: Utilities
	public final void setForEachInputParameters() {
		setForEachParameters(inWidth, inHeight);
	}

	public final void setForEachOutputParameters() {
		setForEachParameters(outWidth, outHeight);
	}

	public final BufferedImage newEmptyInputSizedImage() {
		return newEmptyImage(inWidth, inHeight);
	}



	public final BufferedImage newEmptyOutputSizedImage() {
		return newEmptyImage(outWidth, outHeight);
	}

	public final BufferedImage newEmptyImage(int width, int height) {
		return new BufferedImage(width, height, workingImageType);
	}

	public final BufferedImage duplicateWorkingImage() {
		BufferedImage output = newEmptyInputSizedImage();

		forEach((y, x) -> 
		output.setRGB(x, y, workingImage.getRGB(x, y))
				);

		return output;
	}

	/**
	 * @param img - the image to add zeros to at its edges.
	 * @return the Buffered image padded with zeros.
	 */
	public BufferedImage addZeros(BufferedImage img) {

		BufferedImage ans = newEmptyImage(inWidth+2, inHeight+2);


		for (int x = 0; x < ans.getWidth(); x++) {
			for (int y = 0; y < ans.getHeight(); y++) {


				if(( x>0 && x< ans.getWidth() - 1 ) && ( y>0 && y< ans.getHeight() - 1 ) )
				{
					Color currentPixel = new Color(img.getRGB(x-1, y-1));
					ans.setRGB(x, y, currentPixel.getRGB());
				}
			}
		}		
		return ans;

	}
}
