import moment from 'moment';
import { sprintf } from 'sprintf-js';
import { messages } from './gettext';

export default class AccountExpiry {
  private expiry: moment.Moment;

  constructor(isoString: string, locale: string) {
    this.expiry = moment(isoString).locale(locale);
  }

  public hasExpired(): boolean {
    return this.willHaveExpiredAt(new Date());
  }

  public willHaveExpiredAt(date: Date): boolean {
    return this.expiry.isSameOrBefore(date);
  }

  public formattedDate(): string {
    return this.expiry.format('lll');
  }

  public durationUntilExpiry(): string {
    const daysDiff = this.expiry.diff(new Date(), 'days');

    // Below three months we want to show the duration in days. Moments fromNow() method starts
    // measuring duration in months from 26 days and up.
    // https://momentjs.com/docs/#/displaying/fromnow/
    if (daysDiff >= 26 && daysDiff <= 90) {
      return sprintf(
        // TRANSLATORS: The remaining time left on the account measured in days.
        // TRANSLATORS: Available placeholders:
        // TRANSLATORS: %(duration)s - The remaining time measured in days.
        messages.pgettext('account-expiry', '%(duration)s days'),
        { duration: daysDiff },
      );
    } else {
      return this.expiry.fromNow(true);
    }
  }

  public remainingTime(): string {
    const duration = this.durationUntilExpiry();

    return sprintf(
      // TRANSLATORS: The remaining time left on the account displayed across the app.
      // TRANSLATORS: Available placeholders:
      // TRANSLATORS: %(duration)s - a localized remaining time (in minutes, hours, or days) until the account expiry
      messages.pgettext('account-expiry', '%(duration)s left'),
      { duration },
    );
  }
}
