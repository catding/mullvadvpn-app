import * as React from 'react';
import AccountExpiry from '../../shared/account-expiry';
import { messages } from '../../shared/gettext';
import styles, {
  AccountContainer,
  AccountFooter,
  AccountOutOfTime,
  AccountRow,
  AccountRowLabel,
  AccountRowValue,
  StyledContainer,
} from './AccountStyles';
import AccountTokenLabel from './AccountTokenLabel';
import * as AppButton from './AppButton';
import { Layout } from './Layout';
import { BackBarItem, NavigationBar, NavigationItems } from './NavigationBar';
import SettingsHeader, { HeaderTitle } from './SettingsHeader';

import { AccountToken } from '../../shared/daemon-rpc-types';

interface IProps {
  accountToken?: AccountToken;
  accountExpiry?: string;
  expiryLocale: string;
  isOffline: boolean;
  onLogout: () => void;
  onClose: () => void;
  onBuyMore: () => Promise<void>;
}

export default class Account extends React.Component<IProps> {
  public render() {
    return (
      <Layout>
        <StyledContainer>
          <NavigationBar>
            <NavigationItems>
              <BackBarItem action={this.props.onClose}>
                {messages.pgettext('navigation', 'Settings')}
              </BackBarItem>
            </NavigationItems>
          </NavigationBar>

          <AccountContainer>
            <SettingsHeader>
              <HeaderTitle>{messages.pgettext('navigation', 'Account')}</HeaderTitle>
            </SettingsHeader>

            <AccountRow>
              <AccountRowLabel>
                {messages.pgettext('account-view', 'Account number')}
              </AccountRowLabel>
              <AccountRowValue
                as={AccountTokenLabel}
                accountToken={this.props.accountToken || ''}
              />
            </AccountRow>

            <AccountRow>
              <AccountRowLabel>{messages.pgettext('account-view', 'Paid until')}</AccountRowLabel>
              <FormattedAccountExpiry
                expiry={this.props.accountExpiry}
                locale={this.props.expiryLocale}
              />
            </AccountRow>

            <AccountFooter>
              <AppButton.BlockingButton
                disabled={this.props.isOffline}
                onPress={this.props.onBuyMore}>
                <AppButton.GreenButton style={styles.account__buy_button}>
                  <AppButton.Label>
                    {messages.pgettext('account-view', 'Buy more credit')}
                  </AppButton.Label>
                  <AppButton.Icon source="icon-extLink" height={16} width={16} />
                </AppButton.GreenButton>
              </AppButton.BlockingButton>
              <AppButton.RedButton onPress={this.props.onLogout}>
                {messages.pgettext('account-view', 'Log out')}
              </AppButton.RedButton>
            </AccountFooter>
          </AccountContainer>
        </StyledContainer>
      </Layout>
    );
  }
}

function FormattedAccountExpiry(props: { expiry?: string; locale: string }) {
  if (props.expiry) {
    const expiry = new AccountExpiry(props.expiry, props.locale);

    if (expiry.hasExpired()) {
      return (
        <AccountOutOfTime>{messages.pgettext('account-view', 'OUT OF TIME')}</AccountOutOfTime>
      );
    } else {
      return <AccountRowValue>{expiry.formattedDate()}</AccountRowValue>;
    }
  } else {
    return (
      <AccountRowValue>
        {messages.pgettext('account-view', 'Currently unavailable')}
      </AccountRowValue>
    );
  }
}
