package com.floreantpos.ui.views.order;

import java.awt.Frame;
import java.awt.Window;
import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.floreantpos.POSConstants;
import com.floreantpos.actions.SettleTicketAction;
import com.floreantpos.config.TerminalConfig;
import com.floreantpos.main.Application;
import com.floreantpos.model.ActionHistory;
import com.floreantpos.model.MenuCategory;
import com.floreantpos.model.MenuGroup;
import com.floreantpos.model.MenuItem;
import com.floreantpos.model.MenuModifier;
import com.floreantpos.model.OrderType;
import com.floreantpos.model.Restaurant;
import com.floreantpos.model.Ticket;
import com.floreantpos.model.TicketItem;
import com.floreantpos.model.User;
import com.floreantpos.model.dao.ActionHistoryDAO;
import com.floreantpos.model.dao.MenuItemDAO;
import com.floreantpos.model.dao.RestaurantDAO;
import com.floreantpos.model.dao.ShopTableDAO;
import com.floreantpos.model.dao.TicketDAO;
import com.floreantpos.model.dao.UserDAO;
import com.floreantpos.ui.views.SwitchboardView;
import com.floreantpos.ui.views.order.actions.CategorySelectionListener;
import com.floreantpos.ui.views.order.actions.GroupSelectionListener;
import com.floreantpos.ui.views.order.actions.ItemSelectionListener;
import com.floreantpos.ui.views.order.actions.ModifierSelectionListener;
import com.floreantpos.ui.views.order.actions.OrderListener;
import com.floreantpos.util.OrderUtil;

public class OrderController implements OrderListener, CategorySelectionListener, GroupSelectionListener, ItemSelectionListener, ModifierSelectionListener {
	private OrderView orderView;

	public OrderController(OrderView orderView) {
		this.orderView = orderView;

		orderView.getCategoryView().addCategorySelectionListener(this);
		orderView.getGroupView().addGroupSelectionListener(this);
		orderView.getItemView().addItemSelectionListener(this);
		orderView.getOthersView().setItemSelectionListener(this);
		orderView.getModifierView().addModifierSelectionListener(this);
		orderView.getTicketView().addOrderListener(this);
	}

	public void categorySelected(MenuCategory foodCategory) {
		orderView.showView(GroupView.VIEW_NAME);
		orderView.getGroupView().setMenuCategory(foodCategory);
	}

	public void groupSelected(MenuGroup foodGroup) {
		orderView.showView(MenuItemView.VIEW_NAME);
		orderView.getItemView().setMenuGroup(foodGroup);
	}

	public void groupsSelected(List<MenuGroup> foodGroup) {
		orderView.showView(MenuItemView.VIEW_NAME);
		orderView.getItemView().setMenuGroups(foodGroup);
	}

	public void itemSelected(MenuItem menuItem) {
		MenuItemDAO dao = new MenuItemDAO();
		menuItem = dao.initialize(menuItem);

		TicketItem ticketItem = menuItem.convertToTicketItem();
		orderView.getTicketView().addTicketItem(ticketItem);

		if (menuItem.hasModifiers()) {
			ModifierView modifierView = orderView.getModifierView();
			modifierView.setMenuItem(menuItem, ticketItem);
			orderView.showView(ModifierView.VIEW_NAME);
		}
	}

	public void modifierSelected(MenuItem parent, MenuModifier modifier) {
		// TicketItemModifier itemModifier = new TicketItemModifier();
		// itemModifier.setItemId(modifier.getId());
		// itemModifier.setName(modifier.getName());
		// itemModifier.setPrice(modifier.getPrice());
		// itemModifier.setExtraPrice(modifier.getExtraPrice());
		// itemModifier.setMinQuantity(modifier.getMinQuantity());
		// itemModifier.setMaxQuantity(modifier.getMaxQuantity());
		// itemModifier.setTaxRate(modifier.getTax() == null ? 0 :
		// modifier.getTax().getRate());
		//
		// orderView.getTicketView().addModifier(itemModifier);
	}

	public void itemSelectionFinished(MenuGroup parent) {
		MenuCategory menuCategory = parent.getParent();
		GroupView groupView = orderView.getGroupView();
		if (!menuCategory.equals(groupView.getMenuCategory())) {
			groupView.setMenuCategory(menuCategory);
		}
		orderView.showView(GroupView.VIEW_NAME);
	}

	public void modifierSelectionFiniched(MenuItem parent) {
		MenuGroup menuGroup = parent.getParent();
		MenuItemView itemView = orderView.getItemView();
		if (!menuGroup.equals(itemView.getMenuGroup())) {
			itemView.setMenuGroup(menuGroup);
		}
		orderView.showView(MenuItemView.VIEW_NAME);
	}

	public void payOrderSelected(Ticket ticket) {
		new SettleTicketAction(ticket.getId()).execute();

		if (TerminalConfig.isCashierMode()) {
			String message = "Ticket no " + ticket.getId() + " saved. What do you want to do next?";
			if (ticket.isPaid()) {
				message = "Ticket no " + ticket.getId() + " paid. What do you want to do next?";
			}

			OrderUtil.createNewTakeOutOrder(OrderType.TAKE_OUT);
			Window ancestor = SwingUtilities.getWindowAncestor(Application.getPosWindow());
			CashierModeNextActionDialog dialog = new CashierModeNextActionDialog((Frame) ancestor, message);
			dialog.open();
		} else {
			RootView.getInstance().showView(SwitchboardView.VIEW_NAME);
			SwitchboardView.getInstance().updateTicketList();
		}
	}

	public static void assignTicketNumber(Ticket ticket) {
		String serialID = "";
		int startCounter = 1;
		RestaurantDAO resDAO = RestaurantDAO.getInstance();
		Session session = resDAO.createNewSession();
		if (session != null) {
			Transaction tx = session.beginTransaction();
			try {
				Restaurant res = resDAO.findAll().get(0);
				if (res.getStartTime().getDate() != ticket.getCreateDate().getDate()) {
					res.setStartTime(ticket.getCreateDate());
				} else {
					startCounter = res.getStartCounter();
					startCounter++;
				}
				serialID = String.format("%03d", startCounter);
				res.setStartCounter(startCounter);
				resDAO.saveOrUpdate(res);
				tx.commit();
				ticket.setSerialId(serialID);
			} catch (Exception e) {
				tx.rollback();
			} finally {
				session.close();
				// TicketDAO ticketDAO = new TicketDAO();
				// ticketDAO.saveOrUpdate(ticket);
			}
		}
	}

	// VERIFIED
	public synchronized static void saveOrder(Ticket ticket, boolean changeTicketTime) {
		if (ticket == null)
			return;

		boolean newTicket = ticket.getId() == null;

		TicketDAO ticketDAO = new TicketDAO();
		ticketDAO.saveOrUpdate(ticket, changeTicketTime);

		// save the action
		ActionHistoryDAO actionHistoryDAO = ActionHistoryDAO.getInstance();
		User user = Application.getCurrentUser();

		if (newTicket) {
			ShopTableDAO.getInstance().occupyTables(ticket);

			actionHistoryDAO.saveHistory(user, ActionHistory.NEW_CHECK, POSConstants.RECEIPT_REPORT_TICKET_NO_LABEL + ":" + ticket.getId());
		} else {
			actionHistoryDAO.saveHistory(user, ActionHistory.EDIT_CHECK, POSConstants.RECEIPT_REPORT_TICKET_NO_LABEL + ":" + ticket.getId());
		}
	}

	public synchronized static void closeOrder(Ticket ticket) {
		ticket.setClosed(true);
		ticket.setClosingDate(new Date());

		TicketDAO ticketDAO = new TicketDAO();
		ticketDAO.saveOrUpdate(ticket, true);

		User driver = ticket.getAssignedDriver();
		if (driver != null) {
			driver.setAvailableForDelivery(true);
			UserDAO.getInstance().saveOrUpdate(driver);
		}
	}

	@Override
	public void itemSelectionFinished(List<MenuGroup> parent) {
		// TODO Auto-generated method stub

	}
}
