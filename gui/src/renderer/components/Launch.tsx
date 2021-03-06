import * as React from 'react';
import { Component, Styles, Text, View } from 'reactxp';
import styled from 'styled-components';
import { colors } from '../../config.json';
import { messages } from '../../shared/gettext';
import { SettingsBarButton } from './HeaderBar';
import ImageView from './ImageView';
import { Container, Header, Layout } from './Layout';

const styles = {
  container: Styles.createViewStyle({
    flex: 1,
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: -150,
  }),
  title: Styles.createTextStyle({
    fontFamily: 'DINPro',
    fontSize: 24,
    fontWeight: '900',
    lineHeight: 30,
    letterSpacing: -0.5,
    color: colors.white60,
    marginBottom: 4,
  }),
  subtitle: Styles.createTextStyle({
    fontFamily: 'Open Sans',
    fontSize: 14,
    lineHeight: 20,
    color: colors.white40,
  }),
};

const Logo = styled(ImageView)({
  marginBottom: '5px',
});

interface IProps {
  openSettings: () => void;
}

export default class Launch extends Component<IProps> {
  public render() {
    return (
      <Layout>
        <Header>
          <SettingsBarButton onPress={this.props.openSettings} />
        </Header>
        <Container>
          <View style={styles.container}>
            <Logo height={106} width={106} source="logo-icon" />
            <Text style={styles.title}>{messages.pgettext('generic', 'MULLVAD VPN')}</Text>
            <Text style={styles.subtitle}>
              {messages.pgettext('launch-view', 'Connecting to Mullvad system service...')}
            </Text>
          </View>
        </Container>
      </Layout>
    );
  }
}
