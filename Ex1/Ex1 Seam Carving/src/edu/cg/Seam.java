package edu.cg;


public class Seam
{
		private int start_X_Cordinate;
		private long cost;
		public long getCost() {
			return cost;
		}

		public void setCost(long cost) {
			this.cost = cost;
		}

		private int[] X_Cordinates;
		
		
		/**
		 * First Constructor, initializes a seam accordingly to a given start_X_Cordinate and a height.
		 * @param start_X_Cordinate
		 * @param imageHeight
		 */
		public Seam(int start_X_Cordinate, int imageHeight) {
			this.start_X_Cordinate = start_X_Cordinate;
			this.X_Cordinates = new int[imageHeight];
			
			this.addNewIndex(imageHeight - 1, start_X_Cordinate);
		}
		
		/**
		 * Second Seam constructor, initializes to an existing Seam.
		 * @param routStartingColumnIndex
		 * @param imageHeight
		 * @param columnsIndexList
		 */
		public Seam(int routStartingColumnIndex, int imageHeight, int[] columnsIndexList) {
			this.start_X_Cordinate = routStartingColumnIndex;
			this.X_Cordinates = new int[imageHeight];
			
			this.addNewIndex(imageHeight - 1, routStartingColumnIndex);
		}

		public int[] getX_Cordinates() {
			return X_Cordinates;
		}

		public void setColumnIndexList(int[] columnIndexList) {
			this.X_Cordinates = columnIndexList;
		}

		
		
		
		
		public void addNewIndex(int rowIndex, int columnIndex) {
			this.X_Cordinates[rowIndex] = columnIndex;
		}
		
		public void updateCost(int cost) {
			this.cost += (long) cost;
		}
		
		/**
		 * @returns the new duplicated Seam.
		 */
		public Seam duplicate(){
			Seam newSeam = new Seam(this.start_X_Cordinate,this.X_Cordinates.length);
			newSeam.cost = this.cost;
			for (int i = 0; i < X_Cordinates.length; i++) {
				newSeam.X_Cordinates[i] = this.X_Cordinates[i];
			}
			return newSeam;
		}
		
		/* returns a String which describes the Seam object.
		 */
		public String toString() {
			String s = "";
			s += "(cost = " + cost +  ", routStartingColumnIndex = " + start_X_Cordinate + ", [";
			for (int i = 0; i < X_Cordinates.length - 1; i++) {
				s += X_Cordinates[i] + ", ";
			}
			s+= X_Cordinates[X_Cordinates.length - 1] + "])";
			return s;
		}
		
}
