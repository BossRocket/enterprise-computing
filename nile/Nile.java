/*	Name: Patrick Maley
	Course: CNT 4714 - Fall 2021
	Assignment Title: Project 1 - Event-driven Enterprise Simulation
	Date: Sunday September 12, 2021
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.math.*;
import java.text.*;


class Item
{
	String id;
	String title;
	String price;
	String quantity;
	String discount;
	String subtotal;
	boolean confirmed = false;
}

public class Nile implements ActionListener
{
	private static final int WINDOW_WIDTH = 450; // Pixels
	private static final int WINDOW_HEIGHT = 650; // Pixels
	private static final int FIELD_WIDTH = 40; // Characters
	private static final int AREA_WIDTH = 40; // Characters

	private static final FlowLayout LAYOUT_STYLE = new FlowLayout(FlowLayout.CENTER, 20, 20);

	//instance variables
	//window for GUI
	private JFrame window = new JFrame("Nile.com");

	//user entry areas and display areas
	private JLabel itemNumberTag = new JLabel("Number of items for this order:");
	private JTextField itemNumberText = new JTextField(FIELD_WIDTH);
	private JLabel itemIDTag = new JLabel("Item ID:");
	private JTextField itemIDText = new JTextField(FIELD_WIDTH);
	private JLabel itemQuantityTag = new JLabel("Quantity of item:");
	private JTextField  itemQuantityText = new JTextField(FIELD_WIDTH);
	private JLabel itemInfoTag = new JLabel("Item Info:");
	private JTextField  itemInfoText = new JTextField(FIELD_WIDTH);
	private JLabel orderTotalTag = new JLabel("Order Subtotal:");
	private JTextField  orderTotalText = new JTextField(FIELD_WIDTH);

	private int totalNumberItems = 0;
	private int itemsAttempted = 0;
	private ArrayList<Item> order = new ArrayList<>();
	private ArrayList<String> listOfOrders = new ArrayList<>();
	private FileWriter writer = new FileWriter("transactions.txt", true);
	private File file = new File("inventory.txt");
	private DecimalFormat formatter = new DecimalFormat("#0.00");

	// application buttons
	private JButton processItem = new JButton(new AbstractAction("Process Item")
	{
		//process item
		public void actionPerformed(ActionEvent e)
		{
			totalNumberItems = Integer.parseInt(itemNumberText.getText());

			if (totalNumberItems <= 0)
			{
				JOptionPane.showMessageDialog(window, "Please enter the number number of items in the order.");
				return;
			}

			// System.out.println(totalNumberItems);
			itemNumberText.setEditable(false);

			Item item = new Item();

			try {
				Scanner scanner = new Scanner(file);
				while(scanner.hasNextLine())
				{
					String str = scanner.nextLine();
					String[] tokens = str.split(",");

					if (tokens[0].equals(itemIDText.getText()))
					{
						// If item is out of stock, don't register it in the order.
						if (tokens[2].equals("false"))
							break;

						// Add item contents to Item object.
						item.id = tokens[0];
						item.title = tokens[1];
						item.price = tokens[3].replace(" ", "");
						item.quantity = itemQuantityText.getText();
						item.discount = calculateDiscount(item.quantity, item.price);
						item.subtotal = calculateSubtotal(item.price, item.discount, item.quantity);

						order.add(item);

						String info = "";
						info = info.concat(item.id + " ");
						info = info.concat(item.title + " ");
						info = info.concat("$" + item.price + " ");
						info = info.concat(item.quantity + " ");
						info = info.concat(item.discount + "% ");
						info = info.concat("$" + item.subtotal);
						listOfOrders.add(order.size() + ". " + info);
						itemInfoText.setText(info);

						break;
					}
				}
			} catch (FileNotFoundException ex) {
				System.out.println("FileNotFoundException Caught!");
			}

			if (item.id == null)
			{
				JOptionPane.showMessageDialog(window, "Item not found or out of stock.");
				return;
			}

			processItem.setEnabled(false);
			confirmItem.setEnabled(true);
			itemsAttempted++;
		}
	});

	private JButton confirmItem = new JButton(new AbstractAction("Confirm Item")
	{
		public void actionPerformed(ActionEvent e)
		{
			//confirm item
			if (itemsAttempted != order.size())
			{
				JOptionPane.showMessageDialog(window, "Invalid Item, cannot confirm");
			}

			order.get(itemsAttempted - 1).confirmed = true;
			JOptionPane.showMessageDialog(window, "Item #" + itemsAttempted + " has been added to your order.");

			if (order.size() < totalNumberItems)
				processItem.setEnabled(true);

			viewOrder.setEnabled(true);
			finishOrder.setEnabled(true);
			confirmItem.setEnabled(false);
			orderTotalText.setText("$" + orderSubtotal());
			orderTotalTag.setText("Order Total for " + order.size() + " item(s)");
		}
	});

	private JButton viewOrder = new JButton(new AbstractAction("View Order")
	{
		public void actionPerformed(ActionEvent e)
		{
			//view order
			showOrder();
		}
	});

	private JButton finishOrder = new JButton(new AbstractAction("Finish Order")
	{
		public void actionPerformed(ActionEvent e)
		{
			//finish order
			String[] invoice = new String[8 + order.size()];

			Date date = new Date();
			SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yy hh:mm:ss aa zzz");
			String str = dateFormatter.format(date);
			invoice[0] = "Date: " + str  + "\n\n";

			invoice[1] = "Number of items: " + listOfOrders.size() + "\n\n";

			invoice[2] = "Item# / ID / Title / Price / Qty / Disc% / Subtotal:\n\n";

			for(int i = 0; i < listOfOrders.size(); i++)
			{
				invoice[i + 3] = listOfOrders.get(i);
			}

			invoice[listOfOrders.size() + 3] = "\n\n\nOrder subtotal: $" + orderSubtotal() + "\n\n";

			invoice[listOfOrders.size() + 4] = "Tax rate: %6\n\n";

			invoice[listOfOrders.size() + 5] = "Tax amount: $" + calculateTax() + "\n\n";

			invoice[listOfOrders.size() + 6] = "Order total: $" + calculateOrderTotal() + "\n\n";

			invoice[listOfOrders.size() + 7] = "Thanks for shopping at Nile Dot Com!";



			try {
				writeTransactionsList(date);
			} catch (IOException ex) {
				System.out.println("Exception " + ex + " caught.");
			}

			JOptionPane.showMessageDialog(window, invoice);
			window.setVisible(false);
			System.exit(0);
		}
	});

	private JButton newOrder = new JButton(new AbstractAction("New Order")
	{
		public void actionPerformed(ActionEvent e)
		{
			//new order
			try {
				resetOrder();
			} catch (IOException ex) {
				System.out.println("Exception " + ex + " caught!");
			}
		}
	});

	private JButton exitNile = new JButton(new AbstractAction("Exit")
	{
		public void actionPerformed(ActionEvent e)
		{
			// exit();
			System.exit(0);
		}
	});


	// Nile() constructor
	public Nile() throws IOException
	{
		//configure GUI
		window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		itemInfoText.setEditable(false);
		orderTotalText.setEditable(false);
		confirmItem.setEnabled(false);
		finishOrder.setEnabled(false);
		viewOrder.setEnabled(false);

		processItem.addActionListener(this);
		confirmItem.addActionListener(this);
		viewOrder.addActionListener(this);
		finishOrder.addActionListener(this);
		newOrder.addActionListener(this);
		exitNile.addActionListener(this);

		//add components to the container
		Container c = window.getContentPane();
		c.setLayout(LAYOUT_STYLE);
		c.add(itemNumberTag);
		c.add(itemNumberText);
		c.add(itemIDTag);
		c.add(itemIDText);
		c.add(itemQuantityTag);
		c.add(itemQuantityText);
		c.add(itemInfoTag);
		c.add(itemInfoText);
		c.add(itemInfoTag);
		c.add(itemInfoText);
		c.add(orderTotalTag);
		c.add(orderTotalText);
		c.add(processItem);
		c.add(confirmItem);
		c.add(viewOrder);
		c.add(finishOrder);
		c.add(newOrder);
		c.add(exitNile);

		//display GUI
		window.setVisible(true);
	}

	public String calculateDiscount(String quantityString, String priceString)
	{
		priceString = priceString.replace(" ", "");

		double quantity = Double.parseDouble(quantityString);
		double price = Double.parseDouble(priceString);

		if (quantity <= 4)
			return "0";

		else if (quantity <= 9)
			return "10";

		else if (quantity <= 14)
			return "15";

		else
			return "20";
	}

	public String calculateSubtotal(String priceString, String discountString, String quantityString)
	{
		priceString = priceString.replace(" ", "");

		double price = Double.parseDouble(priceString);
		double discount = Double.parseDouble(discountString);
		double quantity = Double.parseDouble(quantityString);

		double total = (price - ((price * discount) / 100)) * quantity;

		return formatter.format(total);
	}

	public String orderSubtotal()
	{
		double total = 0;
		for (int i = 0; i < order.size(); i++)
		{
			total += Double.parseDouble(order.get(i).subtotal);
		}
		return formatter.format(total);
	}

	public String calculateTax()
	{
		double temp = Double.parseDouble(orderSubtotal());

		return formatter.format((temp * 6) / 100);
	}

	public String calculateOrderTotal()
	{
		return formatter.format(Double.parseDouble(orderSubtotal()) + Double.parseDouble(calculateTax()));
	}

	public void showOrder()
	{
		String[] orderString = new String[order.size()];
		Arrays.fill(orderString, "");

		for (int i = 0; i < order.size(); i++)
		{
			orderString[i] = orderString[i].concat((i + 1) + ". ");
			orderString[i] = orderString[i].concat(order.get(i).id);
			orderString[i] = orderString[i].concat(order.get(i).title);
			orderString[i] = orderString[i].concat(" $" + order.get(i).price);
			orderString[i] = orderString[i].concat(" " + order.get(i).quantity);
			orderString[i] = orderString[i].concat(" " + order.get(i).discount + "%");
			orderString[i] = orderString[i].concat(" $" + order.get(i).subtotal);
		}

		JOptionPane.showMessageDialog(window, orderString);
	}

	private void resetOrder() throws IOException, FileNotFoundException
	{
		window.setVisible(false);
		Nile gui = new Nile();
	}

	public void writeTransactionsList(Date date) throws IOException
	{
		SimpleDateFormat uniqueID = new SimpleDateFormat("ddMMyyyyhhmm");
		SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yy, hh:mm:ss aa zzz");

		for (int i = 0; i < order.size(); i++)
		{
			String stringy = (uniqueID.format(date) + ", " + order.get(i).id + ", " +
			order.get(i).title + ", $" + order.get(i).price + ", " + order.get(i).quantity + ", " +
			order.get(i).discount + "%, $" + order.get(i).subtotal + ", " + dateFormatter.format(date));

			writer.write(stringy + "\n");
		}
		writer.flush();
		writer.close();
	}

	public void actionPerformed(ActionEvent e)
	{
		// default action on button press
		// necessary empty method
	}

	//main():  application entry point
	public static void main(String[] args) throws IOException, FileNotFoundException
	{
		Nile gui = new Nile();
	}
}
