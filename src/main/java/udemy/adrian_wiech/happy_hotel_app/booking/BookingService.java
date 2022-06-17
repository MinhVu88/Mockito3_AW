package udemy.adrian_wiech.happy_hotel_app.booking;

import java.time.temporal.ChronoUnit;

public class BookingService {
	private final PaymentService paymentService;
	private final RoomService roomService;
	private final BookingDAO bookingDAO;
	private final MailSender mailSender;

	private final static double BASE_PRICE_USD = 50.0;

	public BookingService(
		PaymentService paymentService,
		RoomService roomService,
		BookingDAO bookingDAO,
		MailSender mailSender
	) {
		super();
		this.paymentService = paymentService;
		this.roomService = roomService;
		this.bookingDAO = bookingDAO;
		this.mailSender = mailSender;
	}

	public int getGuests() {
		return roomService.getAvailableRooms()
										  .stream()
										  .map(room -> room.getCapacity())
										  .reduce(0, Integer::sum);
	}
	
	public double calculatePriceInUSD(BookingRequest bookingRequest) {
		long nights = ChronoUnit.DAYS.between(bookingRequest.getDateFrom(), bookingRequest.getDateTo());

		return BASE_PRICE_USD * bookingRequest.getGuestCount() * nights;
	}
	
	public double calculatePriceInEuro(BookingRequest bookingRequest) {
		long nights = ChronoUnit.DAYS.between(bookingRequest.getDateFrom(), bookingRequest.getDateTo());

		return CurrencyConverter.toEuro(BASE_PRICE_USD * bookingRequest.getGuestCount() * nights);
	}

	public String verifyBooking(BookingRequest bookingRequest) {
		String roomId = roomService.findAvailableRoomId(bookingRequest);

		double price = calculatePriceInUSD(bookingRequest);

		if (bookingRequest.isPrepaid()) {
			paymentService.pay(bookingRequest, price);
		}

		bookingRequest.setRoomId(roomId);

		String bookingId = bookingDAO.save(bookingRequest);

		roomService.book(roomId);

		mailSender.sendBookingConfirmation(bookingId);

		return bookingId;
	}
	
	public void cancelBooking(String id) {
		BookingRequest request = bookingDAO.get(id);

		roomService.unbook(request.getRoomId());

		bookingDAO.delete(id);
	}
}
