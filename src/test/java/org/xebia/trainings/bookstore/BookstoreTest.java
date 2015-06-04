package org.xebia.trainings.bookstore;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xebia.trainings.bookstore.cart.ShoppingCart;
import org.xebia.trainings.bookstore.coupon.InMemoryDiscountService;
import org.xebia.trainings.bookstore.inventory.internal.InMemoryInventory;
import org.xebia.trainings.bookstore.model.Book;
import org.xebia.trainings.bookstore.model.CashDiscountCoupon;
import org.xebia.trainings.bookstore.model.PercentageDiscountCoupon;

public class BookstoreTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private ShoppingCart cart;

	private InMemoryInventory inventory;

	private InMemoryDiscountService discountService; 

	@Before
	public void createShoppingCart(){
		inventory = new InMemoryInventory();
		discountService = new InMemoryDiscountService();
		inventory.add(new Book("Effective Java", 40, 10));
		inventory.add(new Book("Clean Code", 60, 10));
		inventory.add(new Book("Head First Java", 30, 10));
		cart = new ShoppingCart(inventory,discountService);
	}
	
	@Test
	public void projectInitialSetupShouldWork() {
		assertTrue("project dependency should be met.", true);
	}
	
	/*
	 * As a customer
	 * I want the ability to add one or more books to the shopping cart
	 * So that I can checkout them at their actual base price.
	 */

	@Test
	public void givenCartHasOneBook_WhenCustomerCheckoutTheCart_ThenCheckoutAmountShouldBeEqualToBookActualPrice() throws Exception {
		cart.add("Effective Java");

		int amount = cart.checkout();

		assertThat(amount, is(equalTo(40)));
	}

	@Test
	public void givenCartHasThreeBooks_WhenCustomerCheckoutTheCart_ThenCheckoutAmountIsEqualToSumOfAllBookPrices() throws Exception {
		cart.add("Effective Java");
		cart.add("Clean Code");
		cart.add("Head First Java");

		int amount = cart.checkout();

		assertThat(amount, is(equalTo(130)));
	}

	@Test
	public void whenUserCheckoutABookThatDoesNotExistInInventory_ThenExceptionIsThrown() throws Exception {
		expectedException.expectMessage(equalTo("You can't checkout an empty cart!!"));

		cart.checkout();
	}
	
	/*
	 * As a customer
	 * I want the ability to add multiple copies of a book to the shopping cart
	 * So that I can do bulk checkout of a book
	 */

	@Test
	public void givenCartHasThreeCopiesOfABook_WhenCustomerCheckout_ThenCheckoutAmountIsThreeTimesBookPrice() throws Exception {
		cart.add("Effective Java", 3);

		int amount = cart.checkout();

		assertThat(amount, is(equalTo(120)));
	}

	@Test
	public void givenCartHas2CopiesOfA3CopiesOfBAnd4CopiesOfC_WhenCustomerCheckout_ThenCheckoutAmountIsSumOfAllBookCopyPrices() throws Exception {
		cart.add("Effective Java", 2);
		cart.add("Clean Code", 3);
		cart.add("Head First Java", 4);

		int amount = cart.checkout();

		assertThat(amount, is(equalTo(380)));
	}

	@Test
	public void givenThatCartHasOneCopyOfBook_WhenCustomerAddTwoCopiesOfTheSameBookToCartAndCheckout_ThenCheckoutAmountIsThreeTimesBookPrice() throws Exception {
		cart.add("Effective Java");
		cart.add("Effective Java", 2);

		int amount = cart.checkout();

		assertThat(amount, is(equalTo(120)));
	}
	
	/*
	 * As a store supervisor
	 * I want the ability to add book(s) to the inventory
	 * So that customers can only add books to the cart that are in the inventory
	 */
	
	@Test
	public void givenInventoryDoesNotHaveABook_WhenCustomerAddBookToCart_ThenErrorMessageIsShownToTheUser() throws Exception {
		expectedException.expectMessage(is(equalTo("Sorry, 'TDD in Action' not in stock!!")));
		cart.add("TDD in Action");
	}
	
	/*
	 * As a user
	 * I want to be notified when I add more copies than available in inventory
	 */
	
	@Test
	public void givenOnlyTwoCopiesOfABookInTheInventory_WhenUserAddMoreThanTwoCopiesOfThatBookInTheCart_ThenErrorMessageShouldBeShownToTheUser() throws Exception {
		inventory.add(new Book("TDD in Action", 40, 2));
		
		expectedException.expectMessage(is(equalTo("There are not enough copies of 'TDD in Action' in the inventory.")));

		cart.add("TDD in Action", 5);
	}
	
	/*
	 * As a marketing manager
	 * I want to create flat percentage discount coupons
	 * So that users can apply them during checkout and get discounted checkout price and sales improve
	 */
	@Test
	public void givenCustomerHasValidFlatPercentageDiscountCoupon_WhenCustomerAppliesTheCouponDuringCheckout_ThenDiscountIsAppliedToCheckoutAmount() throws Exception {
		cart.add("Effective Java", 2);
		cart.add("Clean Code", 3);

		String couponCode = createFlatPercentageDiscountCoupon(20);
		
		int amount = cart.checkout(couponCode);
		
		assertThat(amount, is(equalTo(208)));
	}
	
	/*
	 * As a marketing manager
	 * I want to create flat cash discount coupon
	 * So that users can apply them during checkout and get discounted checkout price
	 */
	@Test
	public void givenCustomerHasValidFlatCashDiscountCoupon_WhenCustomerAppliesTheCouponDuringCheckout_ThenCheckoutAmountIsReducedByFlatCashDiscountCouponAmount() throws Exception {
		
		cart.add("Effective Java", 2);
		cart.add("Clean Code", 3);
		
		String couponCode = createFlatCashDiscountCoupon(20);
		
		int amount = cart.checkout(couponCode);
		
		assertThat(amount, is(equalTo(240)));
	}
	
	@Test
	public void givenCustomerHasValidFlatCashDiscountCoupon_WhenCheckoutAmountAfterApplyingDiscountCouponIsLessThan60PercentOfCheckoutAmount_ThenErrorMessageIsShownToTheUser() throws Exception {

		cart.add("Effective Java", 1);

		String couponCode = createFlatCashDiscountCoupon(30);
		
		expectedException.expectMessage(is(equalTo("This coupon is not applicable for this checkout amount.")));
		
		cart.checkout(couponCode);
	}
	
	
	private String createFlatCashDiscountCoupon(int amount) {
		Date start = new Date();
		Date end = new Date(start.getTime() + 24L * 60 * 60 * 1000);
		
		String couponCode = discountService.create(new CashDiscountCoupon(amount, start, end));
		return couponCode;
	}
	
	private String createFlatPercentageDiscountCoupon(int discount) {
		Date start = new Date();
		Date end = new Date(start.getTime() + 24L * 60 * 60 * 1000);
		
		String couponCode = discountService.create(new PercentageDiscountCoupon(discount, start, end));
		return couponCode;
	}
	
}
