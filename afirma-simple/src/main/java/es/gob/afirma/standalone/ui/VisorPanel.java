/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.standalone.ui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.signature.SignValidity;
import es.gob.afirma.signature.SignValidity.SIGN_DETAIL_TYPE;
import es.gob.afirma.signature.SignValidity.VALIDITY_ERROR;
import es.gob.afirma.signature.ValidateBinarySignature;
import es.gob.afirma.signature.ValidateXMLSignature;
import es.gob.afirma.standalone.DataAnalizerUtil;
import es.gob.afirma.standalone.LookAndFeelManager;
import es.gob.afirma.standalone.SimpleAfirmaMessages;
import es.gob.afirma.standalone.VisorFirma;

/** Panel para la espera y detecci&oacute;n autom&aacute;tica de insercci&oacute;n de DNIe.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s
 * @author Carlos Gamuci
 */
public final class VisorPanel extends JPanel implements KeyListener {

    /** Version ID */
    private static final long serialVersionUID = 8309157734617505338L;

    private final VisorFirma visorFirma;

    VisorFirma getVisorFirma() {
        return this.visorFirma;
    }


    /** Construye un panel con la informaci&oacute;n extra&iacute;da de una firma. Si no se
     * indica la firma, esta se cargar&aacute; desde un fichero. Es obligatorio introducir
     * alguno de los dos par&aacute;metros.
     * @param signFile Fichero de firma.
     * @param sign Firma.
     * @param vf VisorFirma para las acciones de los botones
     * @param allowReload <code>true</code> si se desea dar a usuario la opci&oacute;n de ver otras firmas en el
     *                    visor carg&aacute;ndolas mediante un bot&oacute;n, <code>false</code> en caso contrario
     */
    public VisorPanel(final File signFile, final byte[] sign, final VisorFirma vf, final boolean allowReload) {
        super(true);
        this.visorFirma = vf;
        createUI(signFile, sign, allowReload);
    }

    private void createUI(final File signFile, final byte[] sign, final boolean addReloadButton) {
        if (!LookAndFeelManager.HIGH_CONTRAST) {
            this.setBackground(LookAndFeelManager.WINDOW_COLOR);
        }
        this.setLayout(new GridBagLayout());
        this.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        openSign(signFile, sign, addReloadButton);
    }

    private void openSign(final File signFile, final byte[] signature, final boolean addReloadButton) {

        if (signFile == null && signature == null) {
            Logger.getLogger("es.gob.afirma").warning("Se ha intentado abrir una firma nula");  //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        byte[] sign = signature != null ?  signature.clone() : null;

        if (sign == null && signFile != null) {
            try {
                final FileInputStream fis = new FileInputStream(signFile);
                sign = AOUtil.getDataFromInputStream(fis);
                fis.close();
            }
            catch (final Exception e) {
                Logger.getLogger("es.gob.afirma").warning("No se ha podido cargar el fichero de firma: " + e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        SignValidity validity = new SignValidity(SIGN_DETAIL_TYPE.UNKNOWN, null);
        if (sign != null) {
            try {
                validity = validateSign(sign);
            } catch (final Exception e) {
                validity = new SignValidity(SIGN_DETAIL_TYPE.KO, null);
            }
        }

        final JPanel resultPanel = new SignResultPanel(validity, this);
        final JPanel dataPanel = new SignDataPanel(signFile, sign, null, null, this);

        final JPanel bottonPanel = new JPanel(true);
        bottonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));

        if (addReloadButton) {
            final JButton openSign = new JButton(SimpleAfirmaMessages.getString("VisorPanel.1")); //$NON-NLS-1$
            openSign.setMnemonic('V');
            bottonPanel.add(openSign);
            openSign.addKeyListener(this);
            openSign.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (VisorPanel.this.getVisorFirma() != null) {
                        VisorPanel.this.getVisorFirma().loadNewSign();
                    }
                }
            });
        }

        final JButton closeVisor = new JButton(SimpleAfirmaMessages.getString("VisorPanel.0")); //$NON-NLS-1$
        closeVisor.setMnemonic('C');
        closeVisor.addKeyListener(this);
        closeVisor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (VisorPanel.this.getVisorFirma() != null) {
                    VisorPanel.this.getVisorFirma().closeApplication(0);
                }
            }
        });
        bottonPanel.add(closeVisor);

        // Establecemos la configuracion de color
        if (!LookAndFeelManager.HIGH_CONTRAST) {
            bottonPanel.setBackground(LookAndFeelManager.WINDOW_COLOR);
        }

        setLayout(new GridBagLayout());

        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.insets = new Insets(11, 11, 11, 11);
        add(resultPanel, c);
        c.weighty = 1.0;
        c.gridy = 1;
        c.insets = new Insets(0, 11, 11, 11);
        add(dataPanel, c);
        c.weighty = 0.0;
        c.gridy = 2;
        c.insets = new Insets(0, 11, 11, 11);
        add(bottonPanel, c);

        repaint();

    }

    /**
     * Comprueba la validez de la firma.
     * @param sign Firma que se desea comprobar.
     * @return {@code true} si la firma es v&acute;lida, {@code false} en caso contrario.
     * @throws IOException Si ocurren problemas relacionados con la lectura de la firma */
    private static SignValidity validateSign(final byte[] sign) throws IOException {
        if (DataAnalizerUtil.isSignedPDF(sign)) {
            return new SignValidity(SIGN_DETAIL_TYPE.UNKNOWN, VALIDITY_ERROR.UNKOWN_VALIDITY_PDF);
        }
        else if (DataAnalizerUtil.isSignedXML(sign)) {
            return ValidateXMLSignature.validate(sign);
        }
        else if(DataAnalizerUtil.isSignedBinary(sign)) {
            return ValidateBinarySignature.validate(sign, null);
        }
        return new SignValidity(SIGN_DETAIL_TYPE.KO, null);
    }

	@Override
	public void keyPressed(final KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE && VisorPanel.this.visorFirma != null) {
			VisorPanel.this.visorFirma.closeApplication(0);
		}
	}

	@Override
	public void keyReleased(final KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(final KeyEvent e) {
		// TODO Auto-generated method stub

	}

}
