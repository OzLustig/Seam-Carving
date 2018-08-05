package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class SeamsCarver extends ImageProcessor {

	//MARK: An inner interface for functional programming.
	@FunctionalInterface
	interface ResizeOperation {
		BufferedImage apply();
	}

	//MARK: Fields
	private int numOfSeams;
	private ResizeOperation resizeOp;
	private BufferedImage greyScaleWorkingImage;

	//TODO: Add some additional fields:

	private Seam[] seamsArray;
	private int seamsCounter=0;
	private int[][] positionMatrix;


	//MARK: Constructor
	public SeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, RGBWeights rgbWeights) {
		super(logger, workingImage, rgbWeights, outWidth, workingImage.getHeight()); 

		numOfSeams = Math.abs(outWidth - inWidth);

		if(inWidth < 2 | inHeight < 2)
			throw new RuntimeException("Can not apply seam carving: workingImage is too small");

		if(numOfSeams > inWidth/2)
			throw new RuntimeException("Can not apply seam carving: too many seams...");

		//Sets resizeOp with an appropriate method reference
		if(outWidth > inWidth)
			resizeOp = this::increaseImageWidth;
		else if(outWidth < inWidth)
			resizeOp = this::reduceImageWidth;
		else
			resizeOp = this::duplicateWorkingImage;

		//TODO: Initialize your additional fields and apply some preliminary calculations:

		this.positionMatrix = setOriginalPositions();
		this.greyScaleWorkingImage = greyscale();

	}

	//MARK: Methods
	public BufferedImage resize() {
		return resizeOp.apply();
	}

	private int[][] setOriginalPositions()
	{
		int[][] originalPositions = new int[inHeight][inWidth];

		forEach((y, x) ->
		{
			originalPositions[y][x] = x;
		});

		return originalPositions.clone();
	}

	//MARK: Unimplemented methods
	/**
	 * The function removes the required amount of seams using a temporary image.
	 * The function chooses the best seam to remove at each iteration, removes it and 
	 * then creates the new image and continues on the operation of removing seams.
	 * @return the new image after reducing its width.
	 */
	private BufferedImage reduceImageWidth() {
		logger.log("Prepareing for reduceImageWidth changing...");

		// Calculate the number of width seams to remove from the original image.
		int numOfWidthSeams = Math.abs(this.inWidth - outWidth);
		BufferedImage tempImage = this.duplicateWorkingImage();
		// Initialize a new Seam object.
		seamsArray= new Seam[numOfWidthSeams];
		seamsCounter=0;
		for (int i = 0; i < numOfWidthSeams; i++) {
			// Find the best seam found in the temporary image.
			Seam bestSeam = findBestSeam(tempImage);
			seamsArray[seamsCounter] = bestSeam;
			seamsCounter++;
			// Remove the best seam to carry on the operation on the new image.
			tempImage = removeSeam(bestSeam, tempImage);
		}

		logger.log("Changing reduceImage done!");
		return tempImage;
	}

	/**
	 * findBestSeam takes an image as input and returns the best seam to remove/duplicate in the given image.
	 * The function uses the lecture costMatrix M and another matrix called Energy matrix.
	 * Using the inverse method taught in class we find the best seam to remove.
	 * @param tempImage - The image the operation takes place on.
	 * @return the best seam to remove/duplicate in the given tempImage.
	 */
	private Seam findBestSeam(BufferedImage tempImage) {
		// We carry on the operation on the gray scale image, accordingly to the algorithm.
		ImageProcessor ip = new ImageProcessor(logger, tempImage, rgbWeights, tempImage.getWidth(), tempImage.getHeight());
		this.greyScaleWorkingImage = ip.greyscale();
		int[][] pixelEnergy = calculatePixelEnergy(tempImage.getWidth(), tempImage.getHeight());
		int[][] costMatrix = calculateCostMatrix(pixelEnergy, tempImage.getWidth(), tempImage.getHeight());
		Seam bestSeam = inverseBestSeam(costMatrix, pixelEnergy);
		return bestSeam;
	}

	/** The function takes a seam to remove and BufferedImage tempImage to remove the seam from.
	 * Using a position matrix to track the "old" positions and then change them into the newly created 
	 * positions which we created by removing a single seam from tempImage.
	 * @param seamToBeRemoved
	 * @param tempImage
	 * @return
	 */
	private BufferedImage removeSeam(Seam seamToBeRemoved, BufferedImage tempImage) {

		int[][] updatedPositionMatrix = new int[tempImage.getHeight()][tempImage.getWidth()-1];
		BufferedImage ans = new BufferedImage(tempImage.getWidth() - 1, tempImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
		int newImageX_Cordinate;
		for (int y = 0; y < tempImage.getHeight(); y++) {
			newImageX_Cordinate = 0;
			for (int x = 0; x < tempImage.getWidth(); x++) {
				// If the current cordinate belongs to a seam don't copy it to the new image.
				if(seamToBeRemoved.getX_Cordinates()[y] != x) {
					updatedPositionMatrix[y][newImageX_Cordinate] = this.positionMatrix[y][x];
					ans.setRGB(newImageX_Cordinate, y, tempImage.getRGB(x, y));
					newImageX_Cordinate++;
				}
			}
			seamToBeRemoved.addNewIndex(y, positionMatrix[y][seamToBeRemoved.getX_Cordinates()[y]]);
		}
		this.positionMatrix = updatedPositionMatrix.clone();
		return ans;
	}

	/**
	 * calculatePixelEnergy calculates the energy Matrix and returns it accordingly to the lectures.
	 * @param width - the image's width.
	 * @param height - the image's height.
	 * @return the energy matrix.
	 */
	private int[][] calculatePixelEnergy(int width, int height) {
		int[][] pixelEnergy = new int[height][width];
		for (int row = 0; row < pixelEnergy.length; row++) {
			for (int column = 0; column < pixelEnergy[0].length; column++) {
				if(column < width - 1) 
				{	
					// on greyscale image REd == Blue == Green
					pixelEnergy[row][column] = Math.abs((new Color(greyScaleWorkingImage.getRGB(column,row)).getRed() -
							(new Color(greyScaleWorkingImage.getRGB(column+1,row)).getRed())));
				}
				else {
					pixelEnergy[row][column] = Math.abs((new Color(greyScaleWorkingImage.getRGB(column,row)).getRed() -
							(new Color(greyScaleWorkingImage.getRGB(column - 1,row)).getRed())));
				}
			}
		}
		return pixelEnergy;
	}

	/**
	 * calculateCostMatrix calculates the matrix M of a cost for every cell in the picture.
	 * @param pixelEnergy - the given energy matrix.
	 * @param width - the image's width.
	 * @param height - the image's height.
	 * @return the matrix M of costs for every pixel in the image.
	 */
	private int[][] calculateCostMatrix(int[][] pixelEnergy, int width, int height) {
		int[][] M = new int[height][width];
		for (int row = 0; row < M.length; row++) {
			for (int column = 0; column < M[0].length; column++) {
				if(row == 0) 
				{
					M[row][column] = pixelEnergy[row][column];
				}
				else if(column == 0 || column == M[0].length -1){
					M[row][column] = pixelEnergy[row][column];
				}

				else {						
					int cR = Math.abs(new Color(greyScaleWorkingImage.getRGB(column + 1, row)).getRed() - new Color(greyScaleWorkingImage.getRGB(column - 1, row)).getRed())
							+ Math.abs(new Color(greyScaleWorkingImage.getRGB(column,row- 1 )).getRed() - new Color(greyScaleWorkingImage.getRGB(column + 1,row)).getRed());

					int cL = Math.abs(new Color(greyScaleWorkingImage.getRGB(column + 1, row)).getRed() - new Color(greyScaleWorkingImage.getRGB(column - 1,row)).getRed())
							+ Math.abs(new Color(greyScaleWorkingImage.getRGB(column, row - 1 )).getRed() - new Color(greyScaleWorkingImage.getRGB(column - 1, row)).getRed());
					int cV = Math.abs(new Color(greyScaleWorkingImage.getRGB(column + 1, row)).getRed() - new Color(greyScaleWorkingImage.getRGB(column - 1,row)).getRed());

					M[row][column] = pixelEnergy[row][column] + Math.min(M[row-1][column-1] + cL, Math.min(M[row-1][column] + cV, M[row-1][column+1] + cR));
				}
			}			
		}

		return M;
	}


	/**
	 * inverseBestSeam returns the inverse of the "best" seam to remove using the Forward method taught in class.
	 * @param M - cost matrix.
	 * @param pixelEnergy - energy matrix.
	 * @return - the seam object which represents the Seam with the minimal cost in the given image.
	 */
	private Seam inverseBestSeam(int[][] M, int[][] pixelEnergy){
		int buttomX_Cordinate = M[M.length - 1][0];
		for (int column = 1; column < M[0].length; column++) {
			if(M[M.length-1][column] < M[M.length-1][buttomX_Cordinate]) {
				buttomX_Cordinate = column;
			}
		}
		Seam currentSeam = new Seam(buttomX_Cordinate, M.length);
		Seam left = inverseBestSeam_helper(M.length-2, buttomX_Cordinate-1, currentSeam.duplicate(),M, pixelEnergy);
		Seam right = inverseBestSeam_helper(M.length-2, buttomX_Cordinate+1, currentSeam.duplicate(),M, pixelEnergy);
		Seam vertical= inverseBestSeam_helper(M.length-2, buttomX_Cordinate, currentSeam.duplicate(),M, pixelEnergy);
		return minSeam(left,right,vertical);
	}

	/**
	 * 
	 * @param y_cordinate
	 * @param x_cordinate
	 * @param currentSeam
	 * @param M
	 * @param pixelEnergy
	 * @return
	 */
	private Seam inverseBestSeam_helper(int y_cordinate, int x_cordinate, Seam currentSeam, int [][] M, int[][] pixelEnergy )
	{
		int cL, cV, cR;
		// Edge case.
		if(x_cordinate<0 || x_cordinate>M[0].length-1)
		{
			currentSeam.setCost(Integer.MAX_VALUE);
			return currentSeam;
		}

		// If reached to the top row of the picture.
		if(y_cordinate == 0) {
			currentSeam.updateCost(M[y_cordinate][x_cordinate]);
			currentSeam.addNewIndex(y_cordinate, x_cordinate);
			return currentSeam;
		}

		// Updating phase.
		currentSeam.updateCost(M[y_cordinate][x_cordinate]);
		currentSeam.addNewIndex(y_cordinate, x_cordinate);

		// If the current x_cordinate is at the most left side of the picture we don't attempt to inverse the upper left seam.
		if(x_cordinate == 0) 
		{	
			cL = M[y_cordinate][x_cordinate];
			cV = M[y_cordinate][x_cordinate];
			cR = M[y_cordinate][x_cordinate];

			if(M[y_cordinate][x_cordinate] == ( pixelEnergy[y_cordinate][x_cordinate] + M[y_cordinate-1][x_cordinate] + cV) )
			{
				return inverseBestSeam_helper(y_cordinate - 1, x_cordinate, currentSeam.duplicate(), M, pixelEnergy);
			}
			else {
				if(M[y_cordinate][x_cordinate] == ( pixelEnergy[y_cordinate][x_cordinate] + M[y_cordinate][x_cordinate+1] + cR) )
				{
					return inverseBestSeam_helper(y_cordinate - 1, x_cordinate + 1, currentSeam.duplicate(), M, pixelEnergy);
				}
				return inverseBestSeam_helper(y_cordinate - 1, x_cordinate, currentSeam.duplicate(), M, pixelEnergy);
			}
		}	
		// If the current x_cordinate is to the most right part of the picture we don't attempt to inverse the upper right seam.
		else if(x_cordinate == M[0].length-1)
		{
			cL = M[y_cordinate][x_cordinate];
			cV = M[y_cordinate][x_cordinate];

			if(M[y_cordinate][x_cordinate] == ( pixelEnergy[y_cordinate][x_cordinate] + M[y_cordinate-1][x_cordinate] + cV) )
				return inverseBestSeam_helper(y_cordinate - 1, x_cordinate, currentSeam.duplicate(), M, pixelEnergy);
			// up left
			else 
			{
				return inverseBestSeam_helper(y_cordinate - 1, x_cordinate-1, currentSeam.duplicate(), M, pixelEnergy);
			}
		}
		else
		{
			cL = Math.abs(new Color(greyScaleWorkingImage.getRGB(x_cordinate + 1, y_cordinate)).getRed() 
					- new Color(greyScaleWorkingImage.getRGB(x_cordinate - 1,y_cordinate)).getRed())
					+ Math.abs(new Color(greyScaleWorkingImage.getRGB(x_cordinate, y_cordinate - 1 )).getRed() 
							- new Color(greyScaleWorkingImage.getRGB(x_cordinate - 1, y_cordinate)).getRed());
			cV = Math.abs(new Color(greyScaleWorkingImage.getRGB(x_cordinate + 1, y_cordinate)).getRed() 
					- new Color(greyScaleWorkingImage.getRGB(x_cordinate - 1,y_cordinate)).getRed());

			// up Vertical
			if(M[y_cordinate][x_cordinate] == ( pixelEnergy[y_cordinate][x_cordinate] + M[y_cordinate-1][x_cordinate] + cV) )
			{
				return inverseBestSeam_helper(y_cordinate - 1, x_cordinate, currentSeam.duplicate(), M, pixelEnergy);
			}
			// up left
			else if(M[y_cordinate][x_cordinate] == ( pixelEnergy[y_cordinate][x_cordinate] + M[y_cordinate-1][x_cordinate-1] + cL) )
			{
				return inverseBestSeam_helper(y_cordinate - 1, x_cordinate-1, currentSeam.duplicate(), M, pixelEnergy);
			}
			else
			{
				// right
				return inverseBestSeam_helper(y_cordinate - 1, x_cordinate, currentSeam.duplicate(), M, pixelEnergy);
			}
		}
	}

	/**
	 * minSeam takes three seams and returns the seam with the minimum cost of the three.
	 * @param left - the left seam.
	 * @param right - the right seam.
	 * @param vertical - the upper seam.
	 * @return - the seam with the minimum cost of the three.
	 */
	private Seam minSeam(Seam left, Seam right, Seam vertical) {
		if(left.getCost() < right.getCost())
		{
			if(left.getCost() < vertical.getCost())
			{
				return left;
			}
			else
			{	
				return vertical;
			}

		}
		else
		{
			if(right.getCost() < vertical.getCost())
				return right;
			else
				return vertical;
		}
	}

	/**
	 * increaseImageWidth finds the number of width seams to remove and then duplicate these to reach to the desired new image's width.
	 * @return - the new increased image.
	 */
	private BufferedImage increaseImageWidth() {
		logger.log("Prepareing for increaseImageWidth changing...");
		BufferedImage ans = newEmptyOutputSizedImage();
		findSeams();
		int[][] duplicatedPixelsMatrix = getDuplicatedPositionsMatrix();
		setForEachParameters(ans.getWidth(), ans.getHeight());
		forEach((y, x) ->
		{
			ans.setRGB(x, y, this.workingImage.getRGB(duplicatedPixelsMatrix[y][x], y));
		});

		logger.log("increaseImageWidth done...");
		return ans;
	}

	/**
	 * @return an array of best seams to remove / duplicate.
	 */
	private Seam[] findSeams() {

		int numberOfWidthSeams = Math.abs(this.inWidth - this.outWidth);
		Seam[] seams = new Seam[numberOfWidthSeams];
		// We work on the gray scale image.
		BufferedImage ans = greyscale();

		for (int i = 0; i < numberOfWidthSeams; i++)
		{
			seams[i] = findBestSeam(ans);
			ans = removeSeam(seams[i],ans);
			this.greyScaleWorkingImage = ans;
		}

		return seams;
	}

	/**
	 * calcDuplicatedPixelsMatrix returns the matrix containing all pixel's positions in the given working image.
	 * @return
	 */
	private int[][] getDuplicatedPositionsMatrix()
	{
		int[][] ans = new int[this.outHeight][this.outWidth];
		for (int y = 0; y < ans.length; y++)
		{
			int originalPosition = 0;
			int positionsMatrixIndex = 0;

			for (int x = 0; x < ans[0].length; x++)
			{
				// If we're in legal indices.
				if(positionsMatrixIndex < positionMatrix[0].length - 1) {
					// If the x,y pixel appears at its original position -> no need to duplicate it.
					if(this.positionMatrix[y][positionsMatrixIndex] == originalPosition)
					{
						// Copy the pixel as is to positionMatrix.
						ans[y][x] = this.positionMatrix[y][positionsMatrixIndex];
						positionsMatrixIndex++;
					}
					// If the x,y pixel doesn't appear at its original position -> duplicate it.
					else
					{
						ans[y][x] = originalPosition;
						x++;
						ans[y][x] = originalPosition;
					}
				}
				else
				{
					// We arrived to the most right pixel in its row in the picture to output back.
					if(x < ans[0].length - 2)
					{
						ans[y][x] = originalPosition;
						x++;
						ans[y][x] = originalPosition;
					}
				}
				originalPosition = originalPosition + 1;
			}
		}

		return ans;
	}

	public BufferedImage showSeams(int seamColorRGB) {
		//TODO: Implement this method (bonus), remove the exception.
		throw new UnimplementedMethodException("showSeams");
	}
}
