import { ValidationAcceptor, ValidationChecks } from 'langium';
import { 
    ArduinoMlAstType,
    App,
    LCDMessage,
    ConstantText,
    BrickValueRef,
} from './generated/ast';
import type { ArduinoMlServices } from './arduino-ml-module';

/**
 * Register custom validation checks.
 */
export function registerValidationChecks(services: ArduinoMlServices) {
    const registry = services.validation.ValidationRegistry;
    const validator = services.validation.ArduinoMlValidator;
    const checks: ValidationChecks<ArduinoMlAstType> = {
        App: validator.checkNothing,
        LCDMessage: validator.checkLCDMessage
    };
    registry.register(checks, validator);
}

/**
 * Implementation of custom validations.
 */
export class ArduinoMlValidator {

    checkNothing(app: App, accept: ValidationAcceptor): void {
        if (app.name) {
            const firstChar = app.name.substring(0, 1);
            if (firstChar.toUpperCase() !== firstChar) {
                accept('warning', 'App name should start with a capital.', { node: app, property: 'name' });
            }
        }
    }

    /**
     * Validation statique du message LCD :
     * - ASCII imprimable uniquement
     * - taille totale <= 32 (écran 16x2)
     */
    checkLCDMessage(msg: LCDMessage, accept: ValidationAcceptor): void {
        const MAX = 32;
        let length = 0;

        for (const part of msg.parts) {
            if (part.$type === 'ConstantText') {
                const raw = (part as ConstantText).value;
                const v = raw.substring(1, raw.length - 1);
                if (!/^[\x20-\x7E]*$/.test(v)) {
                    accept(
                        'error',
                        'Le texte LCD contient des caracteres non ASCII imprimables.',
                        { node: part, property: 'value' }
                    );
                }
                length += v.length;
            }
            if (part.$type === 'BrickValueRef') {
                const brick = (part as BrickValueRef).brick?.ref;
                if (!brick) continue;
                if (brick.$type === 'Sensor') {
                    length += 4; // HIGH ou LOW
                }
                else if (brick.$type === 'Actuator') {
                    length += 3; // ON ou OFF
                }
                else {
                    length += 4; // fallback
                }
            }
        }
        if (length > MAX) {
            accept(
                'error',
                `Message LCD trop long (${length} caracteres, max = ${MAX}).`,
                { node: msg }
            );
        }
    }
}
